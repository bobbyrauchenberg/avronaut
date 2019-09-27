package com.rauchenberg.avronaut.encoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.schema.AvroSchema
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}



private[this] sealed trait EncodeOperation
private[this] final case class FullEncode(schema: Schema, genericRecord: GenericRecord)        extends EncodeOperation
private[this] final case class FieldEncode(schema: Schema.Field, genericRecord: GenericRecord) extends EncodeOperation
private[this] final case class TypeEncode(value: AvroType)                                     extends EncodeOperation


trait Encoder[T] {

  def encode(value: T): Result[AvroType]

}

object Encoder {

  def apply[T](implicit encoder: Encoder[T]) = encoder

  type Typeclass[T] = Encoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[A](ctx: CaseClass[Typeclass, A])(implicit s: AvroSchema[A]): Typeclass[A] =
    new Typeclass[A] {
      override def encode(value: A): Result[AvroType] = {
        val params = ctx.parameters.toList


      }
    }

  implicit val stringEncoder: Encoder[String] = toAvroString

  implicit val booleanEncoder: Encoder[Boolean] = toAvroBool

  implicit val intEncoder: Encoder[Int] = toAvroInt

  implicit val longEncoder: Encoder[Long] = toAvroLong

  implicit val floatEncoder: Encoder[Float] = toAvroFloat

  implicit val doubleEncoder: Encoder[Double] = toAvroDouble

  implicit val bytesEncoder: Encoder[Array[Byte]] = toAvroBytes
}


