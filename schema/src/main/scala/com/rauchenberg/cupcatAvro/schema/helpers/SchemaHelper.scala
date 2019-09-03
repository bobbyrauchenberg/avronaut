package com.rauchenberg.cupcatAvro.schema.helpers

import cats.syntax.option._
import com.rauchenberg.cupcatAvro.schema.{Field, SchemaError, SchemaResult, safe, schemaField}
import org.apache.avro.{JsonProperties, Schema}
import shapeless.{Inl, Inr}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.apache.avro.Schema
import org.json4s.DefaultFormats


import scala.collection.JavaConverters._

object SchemaHelper {

  implicit val formats = DefaultFormats

  def moveDefaultToHead[T](schema: Schema, default: T, schemaTypeOfDefault: Option[Schema.Type]): SchemaResult = {
    val (first, rest) = schema.getTypes.asScala.partition { t =>
      default match {
        case _ => schemaTypeOfDefault == t.getType.some || t.getName == default.toString

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
    case Field(name, doc, Some(p: Product), schema) => schemaField(name, schema, doc, toAvroFormat(p))
    case Field(name, doc, Some(default), schema) => schemaField(name, schema, doc, default)
    case Field(name, doc, None, schema) => schemaField(name, schema, doc)
  }

  private def toAvroFormat[T](t: T) = parse(write(t)).extract[Map[String, Any]].asJava


}
