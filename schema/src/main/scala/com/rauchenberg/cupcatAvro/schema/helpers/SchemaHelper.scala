package com.rauchenberg.cupcatAvro.schema.helpers

import cats.syntax.option._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.schema.{schemaField, _}
import org.apache.avro.{JsonProperties, Schema}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import shapeless.{Inl, Inr}

import scala.collection.JavaConverters._

object SchemaHelper {

  case class Field[T](name: String, doc: Option[String], default: Option[T], schema: Schema)

  implicit val formats = DefaultFormats

  def moveDefaultToHead[T](schema: Schema, default: T): SchemaResult =
    default match {
      case Some(v)  => moveDefaultToHead(schema, v)
      case Inl(v)   => moveDefaultToHead(schema, v)
      case Inr(v)   => moveDefaultToHead(schema, v)
      case Right(v) => moveDefaultToHead(schema, v)
      case Left(v)  => moveDefaultToHead(schema, v)
      case _ =>
        val (first, rest) = schema.getTypes.asScala.partition { t =>
          t.getName.toLowerCase == formatClassName(default.getClass.getSimpleName)
        }
        val union = schemaUnion((first ++ rest).toList)
        schemaUnion((first.headOption.toSeq ++ rest).toList).flatMap { u =>
          safe(schema.getObjectProps.asScala.foreach { case (k, v) => u.addProp(k, v) })
        }
        union
    }

  def makeSchemaField[T](field: Field[T]): Result[Schema.Field] =
    field match {
      case Field(name, doc, Some(default: Map[_, _]), schema) => schemaField(name, schema, doc, default.asJava)
      case Field(name, doc, Some(default: Seq[_]), schema)    => schemaField(name, schema, doc, default.asJava)
      case Field(name, doc, Some(Some(p: Product)), schema)   => schemaField(name, schema, doc, toJavaMap(p))
      case Field(name, doc, Some(Some(default)), schema)      => schemaField(name, schema, doc, default)
      case Field(name, doc, Some(None), schema)               => schemaField(name, schema, doc, JsonProperties.NULL_VALUE)
      case Field(name, doc, Some(Left(default)), schema)      => schemaField(name, schema, doc, default)
      case Field(name, doc, Some(Right(default)), schema)     => schemaField(name, schema, doc, default)
      case Field(name, doc, Some(Inl(default)), schema)       => schemaField(name, schema, doc, default)
      case Field(name, doc, Some(Inr(default)), schema)       => makeSchemaField(Field(name, doc, default.some, schema))
      case Field(name, doc, Some(p: Product), schema) if (isEnum(p, schema)) =>
        schemaField(name, schema, doc, p.toString)
      case Field(name, doc, Some(p: Product), schema) => schemaField(name, schema, doc, toJavaMap(p))
      case Field(name, doc, Some(default), schema)    => schemaField(name, schema, doc, default)
      case Field(name, doc, None, schema)             => schemaField(name, schema, doc)
    }

  def isEnum(p: Product, schema: Schema) = p.productArity == 0 && schema.getType == Schema.Type.ENUM

  private def formatClassName(s: String) = {
    val className = (if (s.endsWith("$")) s.dropRight(1) else s).toLowerCase
    className match {
      case "integer" => "int"
      case "byte[]"  => "bytes"
      case other     => other
    }
  }

  private def toJavaMap[T](t: T) = parse(write(t)).extract[Map[String, Any]].asJava

}
