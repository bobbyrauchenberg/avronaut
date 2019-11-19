package com.rauchenberg.avronaut.decoder

import com.rauchenberg.avronaut.common.Results
import com.rauchenberg.avronaut.schema.{Parser, SchemaBuilder, SchemaData}
import org.apache.avro.generic.GenericRecord

trait Decoder[A] {

  def data: Results[Decodable[A]]

}

object Decoder {

  def apply[A](implicit decoder: DecoderBuilder[A], schemaBuilder: SchemaBuilder[A]) = new Decoder[A] {
    override val data: Results[Decodable[A]] = schemaBuilder.schema.flatMap(Parser(_).parse).map { schemaData =>
      Decodable(decoder, schemaData)
    }
  }

  def decode[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    runDecoder(genericRecord, decoder, true)

  def decodeAccumulating[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    runDecoder(genericRecord, decoder, false)

  def runDecoder[A, B](a: B, decoder: Decoder[A], failFast: Boolean): Results[A] =
    decoder.data.flatMap { decodable =>
      decodable.decoder.apply(a, decodable.schemaData, failFast)
    }

  implicit class DecodeSyntax[A](val genericRecord: GenericRecord) extends AnyVal {
    def decode(implicit decoder: Decoder[A])             = Decoder.decode(genericRecord, decoder)
    def decodeAccumulating(implicit decoder: Decoder[A]) = Decoder.decodeAccumulating(genericRecord, decoder)
  }

}
