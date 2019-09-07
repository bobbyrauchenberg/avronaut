package com.rauchenberg.cupcatAvro.decoder.instances

import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.decoder.Decoder
import org.apache.avro.generic.GenericRecord

object arrayInstances extends arrayInstances

trait arrayInstances {

  implicit def listDecoder[T] = new Decoder[List[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[List[T]] =
      safe(record.get(fieldName).asInstanceOf[List[T]])
  }

  implicit def seqDecoder[T] = new Decoder[Seq[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Seq[T]] =
      safe(record.get(fieldName).asInstanceOf[Seq[T]])
  }

  implicit def vectorDecoder[T] = new Decoder[Vector[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Vector[T]] =
      safe(record.get(fieldName).asInstanceOf[Vector[T]])
  }
}
