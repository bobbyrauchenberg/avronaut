package com.rauchenberg.avronaut.encoder

import collection.JavaConverters._
import cats.implicits._
import com.rauchenberg.avronaut.common.Avro.{
  fromAvroBoolean,
  fromAvroBytes,
  fromAvroDouble,
  fromAvroFloat,
  fromAvroInt,
  fromAvroLong,
  fromAvroNull,
  fromAvroString
}
import com.rauchenberg.avronaut.common.{Avro, AvroArray, AvroRecord, AvroUnion, Error, Result}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type.{ARRAY, BOOLEAN, BYTES, DOUBLE, FLOAT, INT, LONG, NULL, RECORD, STRING, UNION}
import org.apache.avro.generic.GenericData
import shapeless.{:+:, CNil, Coproduct, Inl}
import shapeless._
import shapeless.ops.coproduct.Inject
import ListParser._

case class ListParser(val schema: Schema, array: AvroArray) {

  type CP =
    Error :+: Int :+: Boolean :+: Array[Byte] :+: Double :+: Float :+: Long :+: String :+: None.type :+: GenericData.Record :+: Result[
      java.util.List[Any]] :+: CNil

  implicit object folder extends Poly1 {
    implicit def caseError         = at[Error](v => v.asLeft)
    implicit def caseInt           = at[Int](_.asRight)
    implicit def caseBoolean       = at[Boolean](_.asRight)
    implicit def caseArrayByte     = at[Array[Byte]](_.asRight)
    implicit def caseDouble        = at[Double](_.asRight)
    implicit def caseFloat         = at[Float](_.asRight)
    implicit def caseLong          = at[Long](_.asRight)
    implicit def caseString        = at[String](_.asRight)
    implicit def caseNone          = at[None.type](_ => Right(null))
    implicit def caseGenericRecord = at[GenericData.Record](_.asRight)
    implicit def caseListAny       = at[Result[java.util.List[Any]]](identity)
  }

  def parse: CP =
    Coproduct[CP] {
      array.value.traverse { value =>
        parseType(schema, value).fold(folder)
      }.map(_.asJava)
    }

  private def parseType(schema: Schema, avroType: Avro): CP = {
    val schemaType = schema.getType
    (schemaType, avroType) match {
      case (RECORD, v @ AvroRecord(_)) =>
        Parser(new GenericData.Record(schema)).parse(v).fold(e => e.toCP[CP], v => v.toCP[CP])
      case (ARRAY, v @ AvroArray(_)) => parseArray(schema, v)
      case (UNION, v @ AvroUnion(_)) => parseUnion(schema, v)
      case (_, _)                    => addPrimitive(schema.getElementType, avroType)
    }
  }

  private def addPrimitive(schema: Schema, value: Avro): CP =
    schema.getType match {
      case STRING  => fromAvroString(value).fold(e => e.toCP[CP], _.toCP[CP])
      case INT     => fromAvroInt(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case LONG    => fromAvroLong(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case FLOAT   => fromAvroFloat(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case DOUBLE  => fromAvroDouble(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case BOOLEAN => fromAvroBoolean(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case BYTES   => fromAvroBytes(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case NULL    => fromAvroNull(value).fold(e => e.toCP[CP], v => v.toCP[CP])
      case _       => Coproduct[CP](Error(s"couldn't map to AST '$value', '$schema'"))
    }

  private def parseUnion(schema: Schema, avroUnion: AvroUnion): CP =
    schema.getTypes.asScala.toList.foldLeft(Error("").toCP[CP]) {
      case (acc, _) =>
        (schema.getType match {
          case UNION => parseType(schema, avroUnion.value)
          case ARRAY => parseType(schema, avroUnion.value)
          case _     => addPrimitive(schema, avroUnion.value)
        }) match {
          case Inl(_) => acc
          case other  => other
        }
    }

  private def parseArray(schema: Schema, avroArray: AvroArray): CP =
    schema.getElementType.getType match {
      case ARRAY =>
        ListParser(schema.getElementType, avroArray).parse
      case _ =>
        avroArray.value.traverse { value =>
          (schema.getElementType.getType match {
            case UNION  => parseType(schema.getElementType, value)
            case RECORD => parseType(schema.getElementType, value)
            case ARRAY  => ListParser(schema.getElementType, avroArray).parse
            case _      => addPrimitive(schema.getElementType, value)
          }).fold(folder)
        }.map(_.asJava).toCP[CP]
    }

}

object ListParser {

  implicit class CoproductOps[T](val t: T) extends AnyVal {
    def toCP[U <: Coproduct](implicit inj: Inject[U, T]): U = Coproduct[U](t)
  }

}
