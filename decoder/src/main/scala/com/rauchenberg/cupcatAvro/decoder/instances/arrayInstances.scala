package com.rauchenberg.cupcatAvro.decoder.instances

import com.rauchenberg.cupcatAvro.decoder.{DecodeResult, Decoder}
import org.apache.avro.generic.GenericRecord
import com.rauchenberg.cupcatAvro.decoder._

object arrayInstances extends arrayInstances

//will need to change these to decode T properly
trait arrayInstances {

  implicit def listDecoder[T] = new Decoder[List[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[List[T]] =
      safe(record.get(fieldName).asInstanceOf[List[T]])
  }

  implicit def seqDecoder[T] = new Decoder[Seq[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Seq[T]] =
      safe(record.get(fieldName).asInstanceOf[Seq[T]])
  }

  implicit def vectorDecoder[T] = new Decoder[Vector[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Vector[T]] =
      safe(record.get(fieldName).asInstanceOf[Vector[T]])
  }
}
