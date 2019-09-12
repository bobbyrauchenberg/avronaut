package com.rauchenberg.cupcatAvro.decoder.instances

import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.decoder.Decoder
import org.apache.avro.generic.GenericRecord

object mapInstances extends mapInstances

trait mapInstances {

  implicit def mapDecoder[T] = new Decoder[Map[String, T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Map[String, T]] =
      safe(record.get(fieldName).asInstanceOf[Map[String, T]])
  }

}
