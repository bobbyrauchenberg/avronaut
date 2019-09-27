package com.rauchenberg.avronaut.encoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.schema.AvroSchema
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

private[this] sealed trait EncodeOperation
private[this] final case class FullEncode(schema: Schema, genericRecord: GenericRecord)        extends EncodeOperation
private[this] final case class FieldEncode(schema: Schema.Field, genericRecord: GenericRecord) extends EncodeOperation
private[this] final case class TypeEncode(value: AvroType)                                     extends EncodeOperation

trait Encoder[A] {

  def encode(value: A): Result[AvroType]

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def encode[A](a: A)(implicit encoder: Encoder[A], schema: AvroSchema[A]): Either[Error, GenericRecord] =
    for {
      s       <- schema.schema
      encoded <- encoder.encode(a)
      genRec  <- Parser.parse(s, encoded.asInstanceOf[AvroRecord])
    } yield genRec

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] =
    new Typeclass[A] {
      override def encode(value: A): Result[AvroType] = {
        val params = ctx.parameters.toList
        val res = params.traverse {
          case param =>
            val res = param.typeclass.encode(param.dereference(value))
            println(res)
            res
        }
        res.map(AvroRecord(_))
      }
    }

  implicit val stringEncoder: Encoder[String] = toAvroString

  implicit val booleanEncoder: Encoder[Boolean] = toAvroBoolean

  implicit val intEncoder: Encoder[Int] = toAvroInt

  implicit val longEncoder: Encoder[Long] = toAvroLong

  implicit val floatEncoder: Encoder[Float] = toAvroFloat

  implicit val doubleEncoder: Encoder[Double] = toAvroDouble

  implicit val bytesEncoder: Encoder[Array[Byte]] = toAvroBytes

  implicit def listEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[List[A]] =
    value => value.traverse(elementEncoder.encode(_)).map(AvroArray(_))
}
