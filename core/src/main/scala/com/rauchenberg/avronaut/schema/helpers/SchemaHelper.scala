package com.rauchenberg.avronaut.schema.helpers

import java.time.OffsetDateTime

import cats.syntax.option._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.schema.{schemaField, _}
import org.apache.avro.{JsonProperties, Schema}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import shapeless.{Inl, Inr}

import scala.collection.JavaConverters._

object SchemaHelper {

  case class Field[A](name: String, doc: Option[String], default: Option[A], schema: Schema)

  implicit val formats = DefaultFormats

  def moveDefaultToHead[A](schema: Schema, default: A): SchemaResult =
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

  def transformDefault[A](default: A, schema: Schema): Any =
    default match {
      case default: Map[_, _]              => default.asJava
      case default: Seq[_]                 => default.asJava
      case Some(p: Product)                => toJavaMap(p)
      case Left(p: Product)                => toJavaMap(p)
      case Right(p: Product)               => toJavaMap(p)
      case Some(default)                   => default
      case None                            => JsonProperties.NULL_VALUE
      case Left(default)                   => default
      case Right(default)                  => default
      case Inl(default)                    => default
      case Inr(default)                    => transformDefault(default, schema)
      case p: Product if isEnum(p, schema) => p.toString
      case p: Product                      => toJavaMap(p)
      case odt: OffsetDateTime             => odt.toInstant.toEpochMilli
      case default                         => default
    }

  def makeSchemaField[A](field: Field[A]): Result[Schema.Field] =
    field match {
      case Field(name, doc, Some(default: Map[_, _]), schema) => schemaField(name, schema, doc, default.asJava)
      case Field(name, doc, Some(default: Seq[_]), schema)    => schemaField(name, schema, doc, default.asJava)
      case Field(name, doc, Some(Some(p: Product)), schema)   => schemaField(name, schema, doc, toJavaMap(p))
      case Field(name, doc, Some(Left(p: Product)), schema)   => schemaField(name, schema, doc, toJavaMap(p))
      case Field(name, doc, Some(Right(p: Product)), schema)  => schemaField(name, schema, doc, toJavaMap(p))
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

  private def toJavaMap[A](t: A): java.util.Map[String, Any] =
    parse(write(t))
      .extract[Map[String, Any]]
      .map {
        case (k, v) =>
          k -> (v match {
            case x: Int       => java.lang.Integer.valueOf(x)
            case x: BigInt    => java.lang.Double.valueOf(x.doubleValue)
            case x: Float     => java.lang.Float.valueOf(x)
            case x: Double    => java.lang.Double.valueOf(x)
            case x: Long      => java.lang.Long.valueOf(x)
            case x: String    => new java.lang.String(x)
            case x: Map[_, _] => toJavaMap(x)
            case other        => other
          })
      }
      .asJava

}
