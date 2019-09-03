package com.rauchenberg.cupcatAvro.schema

import cats.implicits._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations._
import org.apache.avro.{JsonProperties, Schema}
import magnolia.{CaseClass, Magnolia, Param}
import SchemaHelper._

case class Field[T](name: String, doc: String, default: Option[T], schema: Schema)

trait AvroSchema[T] {
  def schema: SchemaResult
}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](cc: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {

      val annotations = getAnnotations(cc.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, cc.typeName.short, cc.typeName.owner)
      val doc = getDoc(annotations)

      cc.parameters.toList.traverse { param =>
        for {
          schema <- param.typeclass.schema
          field <- (toField(param, schema))
          schemaField <- makeSchemaField(field)
        } yield schemaField
      }.flatMap(fields => schemaRecord(name, doc, namespace, false, fields))
    }
  }

  private def toField[T](param: Param[Typeclass, T], schema: Schema): Either[SchemaError, Field[T]] = {
    val annotations = getAnnotations(param.annotations)
    val name = getName(annotations, param.label)
    val doc = getDoc(annotations)
    val default = param.default.asInstanceOf[Option[T]]

    schema.getType match {
      case Schema.Type.UNION =>
        default.traverse { defaultValue =>
          moveDefaultToHead(schema, defaultValue, schemaFor(default)).map(Field(name, doc, default, _))
        }.map(_.getOrElse(Field(name, doc, None, schema)))
      case _ => Field(name, doc, default, schema).asRight[SchemaError]
    }
  }

  private def makeSchemaField[T](field: Field[T]): Either[SchemaError, Schema.Field] = field match {
    case Field(name, doc, Some(Some(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(None), schema) => schemaField(name, schema, doc, JsonProperties.NULL_VALUE)
    case Field(name, doc, Some(Left(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(Right(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(default), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, None, schema) => schemaField(name, schema, doc)
  }

}

