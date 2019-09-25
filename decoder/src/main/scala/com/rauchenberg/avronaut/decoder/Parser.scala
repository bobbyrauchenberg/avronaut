package com.rauchenberg.avronaut.decoder

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.json4s.DefaultFormats

object Parser {

  implicit val formats = DefaultFormats

  def decode[A](readerSchema: Schema, genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    decoder.apply(FullDecode(readerSchema, genericRecord))

}
