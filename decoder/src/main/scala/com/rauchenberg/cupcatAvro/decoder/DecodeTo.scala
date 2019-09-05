package com.rauchenberg.cupcatAvro.decoder

import org.apache.avro.generic.GenericRecord

object DecodeTo {

  def apply[T](record: GenericRecord)(implicit decoder: Decoder[T]) = decoder.decodeFrom("", record)

}
