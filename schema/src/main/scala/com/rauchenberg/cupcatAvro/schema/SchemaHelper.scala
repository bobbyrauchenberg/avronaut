package com.rauchenberg.cupcatAvro.schema

import cats.syntax.option._
import org.apache.avro.generic.GenericData
import org.apache.avro.{JsonProperties, Schema}

import scala.collection.JavaConverters._

object SchemaHelper {

  def schemaFor[T](t: T): Option[Schema.Type] = {
    t match {
      case Some(v) => schemaFor(v)
      case _: String => Schema.Type.STRING.some
      case _: Long => Schema.Type.LONG.some
      case _: Int => Schema.Type.INT.some
      case _: Boolean => Schema.Type.BOOLEAN.some
      case _: Float => Schema.Type.FLOAT.some
      case _: Double => Schema.Type.DOUBLE.some
      case _: Array[Byte] => Schema.Type.BYTES.some
      case _: GenericData.EnumSymbol => Schema.Type.ENUM.some
      case _: java.util.Collection[_] => Schema.Type.ARRAY.some
      case _: java.util.Map[_, _] => Schema.Type.MAP.some
      case _: Map[_, _] => Schema.Type.MAP.some
      case _: Seq[_] => Schema.Type.ARRAY.some
      case JsonProperties.NULL_VALUE => Schema.Type.NULL.some
      case _ => None
    }
  }

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

  def extractDefault[T](t: T, schema: Schema): Any = {
    t match {
      case Some(v) => extractDefault(v, schema)
      case other => other
    }
  }


}
