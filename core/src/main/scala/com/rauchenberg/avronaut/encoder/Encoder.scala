package com.rauchenberg.avronaut.encoder

import cats.data.Reader
import cats.implicits._
import com.rauchenberg.avronaut.common.Avro._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.generic.GenericData
import shapeless.{:+:, Coproduct}

import scala.collection.JavaConverters._

trait Encoder[A] {

  def encode(value: A): Reader[SchemaData, Result[Avro]]

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def encode[A](a: A, schemaData: SchemaData)(implicit encoder: Encoder[A]): Either[Error, GenericData.Record] =
    for {
      encoded <- encoder.encode(a)(schemaData)
      genRec <- encoded match {
                 case AvroRecord(schema, values) =>
                   Parser(new GenericData.Record(schemaData.schema)).parse(AvroRoot(schema, values))
                 case _ => Error(s"Can only encode records, got $encoded").asLeft
               }
    } yield genRec

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] =
    new Typeclass[A] {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      override def encode(value: A): Reader[SchemaData, Result[Avro]] =
        Reader[SchemaData, Result[Avro]] { schemaData =>
          schemaData.schemaMap.get(s"$namespace.$name") match {
            case None => Error(s"found no schema for type $namespace.$name").asLeft
            case Some(schema) =>
              val record = AvroRecord(schema, _)
              schema.getFields.asScala.toList.traverse { field =>
                ctx.parameters.toList
                  .find(_.label == field.name)
                  .map(_.asRight)
                  .getOrElse(Error(s"couldn't find param for schema field ${field.name}").asLeft)
                  .flatMap { param =>
                    param.typeclass.encode(param.dereference(value)).apply(schemaData)
                  }
              }.map(record)
          }
        }
    }

  implicit val stringEncoder: Encoder[String] = new Encoder[String] {
    override def encode(value: String): Reader[SchemaData, Result[Avro]] = toAvroString(value).liftR
  }

  implicit val booleanEncoder: Encoder[Boolean] = new Encoder[Boolean] {
    override def encode(value: Boolean): Reader[SchemaData, Result[Avro]] = toAvroBoolean(value).liftR
  }

  implicit val intEncoder: Encoder[Int] = new Encoder[Int] {
    override def encode(value: Int): Reader[SchemaData, Result[Avro]] = toAvroInt(value).liftR
  }

  implicit val longEncoder: Encoder[Long] = new Encoder[Long] {
    override def encode(value: Long): Reader[SchemaData, Result[Avro]] = toAvroLong(value).liftR
  }

  implicit val floatEncoder: Encoder[Float] = new Encoder[Float] {
    override def encode(value: Float): Reader[SchemaData, Result[Avro]] = toAvroFloat(value).liftR
  }

  implicit val doubleEncoder: Encoder[Double] = new Encoder[Double] {
    override def encode(value: Double): Reader[SchemaData, Result[Avro]] = toAvroDouble(value).liftR
  }

  implicit val bytesEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
    override def encode(value: Array[Byte]): Reader[SchemaData, Result[Avro]] = toAvroBytes(value).liftR
  }

  implicit def listEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[List[A]] = new Encoder[List[A]] {
    override def encode(value: List[A]): Reader[SchemaData, Result[Avro]] =
      value.traverse(elementEncoder.encode(_)).map(_.sequence.map(AvroArray(_)))
  }

  implicit def optionEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    override def encode(value: Option[A]): Reader[SchemaData, Result[Avro]] = Reader { sd =>
      value.fold[Result[Avro]](toAvroNull(null))(v => elementEncoder.encode(v).apply(sd)).map(AvroUnion(_))
    }
  }

  implicit def mapEncoder[A]: Encoder[Map[String, A]] = ???

  implicit def eitherEncoder[L, R]: Encoder[Either[L, R]] = ???

  implicit def coproductEncoder[H, T <: Coproduct]: Encoder[H :+: T] = ???
}
