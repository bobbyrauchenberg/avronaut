package com.rauchenberg.avronaut.encoder

import cats.implicits._
import com.rauchenberg.avronaut.common.Avro._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.schema.AvroSchema
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.generic.GenericData
import shapeless.{:+:, Coproduct}

import scala.collection.JavaConverters._

trait Encoder[A] {

  def encode(value: A): Result[Avro]

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def encode[A](a: A)(implicit encoder: Encoder[A], schema: AvroSchema[A]): Either[Error, GenericData.Record] =
    for {
      s       <- schema.schema
      encoded <- encoder.encode(a)
      genRec <- encoded match {
                 case AvroRecord(schema, values) =>
                   FoldingParser(new GenericData.Record(s)).parse(AvroRoot(schema, values))
                 case _ => Error(s"Can only encode records, got $encoded").asLeft
               }
    } yield genRec

  def combine[A](ctx: CaseClass[Typeclass, A])(implicit s: AvroSchema[A]): Typeclass[A] =
    new Typeclass[A] {
      override def encode(value: A): Result[Avro] =
        s.schema.flatMap { schema =>
          schema.getFields.asScala.toList.traverse { field =>
            ctx.parameters.toList
              .find(_.label == field.name)
              .map(_.asRight)
              .getOrElse(Error(s"couldn't find param for schema field ${field.name}").asLeft)
              .flatMap { param =>
                param.typeclass.encode(param.dereference(value))
              }
          }.map { AvroRecord(schema, _) }
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
    _.traverse(elementEncoder.encode(_)).map(AvroArray(_))

  implicit def optionEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Option[A]] =
    _.fold[Result[Avro]](toAvroNull(null))(v => elementEncoder.encode(v)).map(AvroUnion(_))

  implicit def mapEncoder[A]: Encoder[Map[String, A]] = ???

  implicit def eitherEncoder[L, R]: Encoder[Either[L, R]] = ???

  implicit def coproductEncoder[H, T <: Coproduct]: Encoder[H :+: T] = ???
}
