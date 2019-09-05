package com.rauchenberg.cupcatAvro.schema.helpers

import org.apache.avro.generic.GenericData
import org.apache.avro.{JsonProperties, Schema}
import shapeless.{Inl, Inr}
import cats.syntax.option._

object AvroTypeMapper {

  def avroTypeFor[T](t: T): Option[Schema.Type] = {
    t match {
      case Some(v) => avroTypeFor(v)
      case None => Schema.Type.NULL.some
      case Inl(v) => avroTypeFor(v)
      case Inr(v) => avroTypeFor(v)
      case Right(v) => avroTypeFor(v)
      case Left(v) => avroTypeFor(v)
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
      case _: Product => Schema.Type.RECORD.some
      case JsonProperties.NULL_VALUE => Schema.Type.NULL.some
      case _ => None
    }
  }

}
