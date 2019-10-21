package com.rauchenberg.avronaut.decoder

import com.rauchenberg.avronaut.common.Results
import org.apache.avro.generic.GenericRecord

trait Decoder[A] {

  def data: Decodable[A]

}

object Decoder {

  def apply[A](implicit decoder: DecoderBuilder[A]) = new Decoder[A] {
    override def data: Decodable[A] = Decodable(decoder)
  }

  def decode[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    decoder.data.decoder(genericRecord, true)

  def decodeAccumulating[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    decoder.data.decoder(genericRecord, false)

  implicit class DecodeSyntax[A](val genericRecord: GenericRecord) extends AnyVal {
    def encode(implicit decoder: Decoder[A])             = Decoder.decode(genericRecord, decoder)
    def encodeAccumulating(implicit decoder: Decoder[A]) = Decoder.decodeAccumulating(genericRecord, decoder)
  }

}
