package com.rauchenberg.cupcatAvro.decoder.instances

import cats.implicits._
import com.rauchenberg.cupcatAvro.decoder.{DecodeResult, Decoder}
import org.apache.avro.generic.GenericRecord
import com.rauchenberg.cupcatAvro.decoder._

object unionInstances extends unionInstances

trait unionInstances {

  implicit def optionDecoder[T](implicit someDecoder: Decoder[T]) = new Decoder[Option[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Option[T]] =
      if (record.get(fieldName) == null) none[T].asRight
      else someDecoder.decodeFrom(fieldName, record).map(Option.apply)
  }

  implicit def eitherDecoder[L, R](implicit leftDecoder: Decoder[L], rightDecoder: Decoder[R]) =
    new Decoder[Either[L, R]] {
      override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Either[L, R]] =
        safe(leftDecoder.decodeFrom(fieldName, record)) match {
          case Right(Right(v)) => v.asLeft.asRight
          case _ => rightDecoder.decodeFrom(fieldName, record) match {
            case Right(Right(v)) => v.asRight[L].asRight[DecodeError]
          }
        }
    }
}

