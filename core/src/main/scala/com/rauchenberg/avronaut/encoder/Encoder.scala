package com.rauchenberg.avronaut.encoder

import cats.syntax.either._
import com.rauchenberg.avronaut.common.{Error, Results}
import com.rauchenberg.avronaut.schema.{Parser, SchemaBuilder}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

trait Encoder[A] {
  def data: Results[Encodable[A]]
}

object Encoder {

  def apply[A](implicit encoderBuilder: EncoderBuilder[A], schemaBuilder: SchemaBuilder[A]): Encoder[A] =
    new Encoder[A] {
      override val data: Results[Encodable[A]] =
        schemaBuilder.schema.flatMap(Parser(_).parse).map { schemaData =>
          Encodable(encoderBuilder, schemaData)
        }
    }

  def encode[A](a: A, encoder: Encoder[A]): Results[GenericRecord] =
    runEncoder(a, encoder, true)

  def encodeAccumulating[A](a: A, encoder: Encoder[A]): Results[GenericRecord] =
    runEncoder(a, encoder, false)

  def schema[A](encoder: Encoder[A]): Results[Schema] =
    encoder.data.map(_.schemaData.schema)

  private def runEncoder[A](a: A, encoder: Encoder[A], failFast: Boolean): Either[List[Error], GenericRecord] =
    encoder.data.flatMap { encodable =>
      encodable.encoder.apply(a, encodable.schemaData, failFast) match {
        case Right(gr: GenericRecord) => Right(gr)
        case Left(l: List[_])         => l.asInstanceOf[List[Error]].asLeft[GenericRecord]
        case _                        => List(Error("something went very wrong :o")).asLeft[GenericRecord]
      }
    }

  implicit class EncodeSyntax[A](val toEncode: A) extends AnyVal {
    def encode(implicit encoder: Encoder[A])             = Encoder.encode(toEncode, encoder)
    def encodeAccumulating(implicit encoder: Encoder[A]) = Encoder.encodeAccumulating(toEncode, encoder)
  }
}
