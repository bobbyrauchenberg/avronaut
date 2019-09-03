package com.rauchenberg.cupcatAvro.schema.helpers

import com.rauchenberg.cupcatAvro.schema.{Field, SchemaError, SchemaResult, safe, schemaField}
import org.apache.avro.{JsonProperties, Schema}

import collection.JavaConverters._
import cats.syntax.option._
import shapeless.{Inl, Inr}

object SchemaHelper {

  def moveDefaultToHead[T](schema: Schema, default: T, schemaTypeOfDefault: Option[Schema.Type]): SchemaResult = {
    val (first, rest) = schema.getTypes.asScala.partition { t =>
      default match {
        case _ => schemaTypeOfDefault == t.getType.some
      }
    }
    safe {
      val result = Schema.createUnion(first.headOption.toSeq ++ rest: _*)
      schema.getObjectProps.asScala.foreach { case (k, v) => result.addProp(k, v) }
      result
    }
  }

  def makeSchemaField[T](field: Field[T]): Either[SchemaError, Schema.Field] = field match {
    case Field(name, doc, Some(Some(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(None), schema) => schemaField(name, schema, doc, JsonProperties.NULL_VALUE)
    case Field(name, doc, Some(Left(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(Right(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(Inl(default)), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, Some(Inr(default)), schema) => makeSchemaField(Field(name, doc, default.some, schema))
    case Field(name, doc, Some(default), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, None, schema) => schemaField(name, schema, doc)
  }

}
