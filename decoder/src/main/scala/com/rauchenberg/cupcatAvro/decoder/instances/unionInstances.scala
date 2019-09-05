package com.rauchenberg.cupcatAvro.decoder.instances

import cats.implicits._
import com.rauchenberg.cupcatAvro.decoder.{DecodeResult, Decoder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import com.rauchenberg.cupcatAvro.decoder._

object unionInstances extends unionInstances

trait unionInstances {

  implicit def optionDecoder[T](implicit someDecoder: Decoder[T]) = new Decoder[Option[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Option[T]] =
      if (record.get(fieldName) == null) DecodeError("got a null decoding option").asLeft
      else someDecoder.decodeFrom(fieldName, record).map(Option.apply)
  }
}

