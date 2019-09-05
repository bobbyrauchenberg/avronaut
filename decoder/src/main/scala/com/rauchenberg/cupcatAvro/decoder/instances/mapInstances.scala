package com.rauchenberg.cupcatAvro.decoder.instances

import com.rauchenberg.cupcatAvro.decoder.{DecodeResult, Decoder}
import org.apache.avro.generic.GenericRecord
import com.rauchenberg.cupcatAvro.decoder._

object mapInstances extends mapInstances

trait mapInstances {

  implicit def mapDecoder[T]  = new Decoder[Map[String, T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[Map[String, T]] =
      safe(record.get(fieldName).asInstanceOf[Map[String, T]])
  }

}
