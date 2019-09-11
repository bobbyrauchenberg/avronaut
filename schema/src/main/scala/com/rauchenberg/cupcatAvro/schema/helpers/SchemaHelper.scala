package com.rauchenberg.cupcatAvro.schema.helpers

import cats.syntax.option._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.schema._
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

  def makeSchemaField[T](field: Field[T]): Result[Schema.Field] = {
    import field.{name, schema, doc}
    field.default match {
      case Some(default: Map[_, _])              => schemaField(name, schema, doc, default.asJava)
      case Some(default: Seq[_])                 => schemaField(name, schema, doc, default.asJava)
      case Some(Some(p: Product))                => schemaField(name, schema, doc, toJavaMap(p))
      case Some(Some(default))                   => schemaField(name, schema, doc, default)
      case Some(None)                            => schemaField(name, schema, doc, JsonProperties.NULL_VALUE)
      case Some(Left(default))                   => schemaField(name, schema, doc, default)
      case Some(Right(default))                  => schemaField(name, schema, doc, default)
      case Some(Inl(default))                    => schemaField(name, schema, doc, default)
      case Some(Inr(default))                    => makeSchemaField(Field(name, doc, default.some, schema))
      case Some(p: Product) if isEnum(p, schema) => schemaField(name, schema, doc, p.toString)
      case Some(p: Product)                      => schemaField(name, schema, doc, toJavaMap(p))
      case Some(default)                         => schemaField(name, schema, doc, default)
      case None                                  => schemaField(name, schema, doc)
    }
  }

  def isEnum(p: Product, schema: Schema) = p.productArity == 0 && schema.getType == Schema.Type.ENUM

  private def formatClassName(s: String) = {
    val className = (if (s.endsWith("$")) s.dropRight(1) else s).toLowerCase
    className match {
      case "integer" => "int"
      case "byte[]" => "bytes"
      case other => other
    }
  }

  private def toJavaMap[T](t: T) = parse(write(t)).extract[Map[String, Any]].asJava

}
