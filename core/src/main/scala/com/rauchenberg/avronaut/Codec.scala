package com.rauchenberg.avronaut

import com.rauchenberg.avronaut.common.Results
import com.rauchenberg.avronaut.decoder.{Decoder, DecoderBuilder}
import com.rauchenberg.avronaut.encoder.{Encoder, EncoderBuilder}
import com.rauchenberg.avronaut.schema.{Parser, SchemaBuilder, SchemaData}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

case class Codecs[A](decoder: Decoder[A], encoder: Encoder[A], schemaData: SchemaData)

trait Codec[A] {
  def codec: Results[Codecs[A]]
}

object Codec {

  def apply[A](implicit schemaBuilder: SchemaBuilder[A],
               encoderBuilder: EncoderBuilder[A],
               decoderBuilder: DecoderBuilder[A]) =
    new Codec[A] {
      def codec: Results[Codecs[A]] = {
        val encoder = Encoder[A]
        val decoder = Decoder[A]
        encoder.data.map { e =>
          Codecs(decoder, encoder, e.schemaData)
        }
      }
    }

  def schema[A](implicit codec: Codec[A]): Results[Schema] = codec.codec.map(_.schemaData.schema)

  def encoder[A](implicit codec: Codec[A]): Results[Encoder[A]] = codec.codec.map(_.encoder)

  def decoder[A](implicit codec: Codec[A]): Results[Decoder[A]] = codec.codec.map(_.decoder)

  implicit class EncodeSyntax[A](val toEncode: A) extends AnyVal {
    def encode(implicit codec: Codec[A]) = codec.codec.flatMap(c => Encoder.encode(toEncode, c.encoder))
    def encodeAccumulating(implicit codec: Codec[A]) =
      codec.codec.flatMap(c => Encoder.encodeAccumulating(toEncode, c.encoder))
  }

  implicit class DecodeSyntax(val genericRecord: GenericRecord) extends AnyVal {
    def decode[A](implicit codec: Codec[A]) = codec.codec.flatMap(c => Decoder.decode(genericRecord, c.decoder))
    def deodeAccumulating[A](implicit codec: Codec[A]) =
      codec.codec.flatMap(c => Decoder.decodeAccumulating(genericRecord, c.decoder))
  }
}
