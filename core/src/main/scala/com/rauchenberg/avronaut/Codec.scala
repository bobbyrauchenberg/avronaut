package com.rauchenberg.avronaut

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.rauchenberg.avronaut.common.safeL
import com.rauchenberg.avronaut.common.Results
import com.rauchenberg.avronaut.decoder.{Decoder, DecoderBuilder}
import com.rauchenberg.avronaut.encoder.{Encoder, EncoderBuilder}
import com.rauchenberg.avronaut.schema.{Parser, SchemaBuilder, SchemaData}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

case class Codecs[A](decoder: Decoder[A], encoder: Encoder[A], schemaData: SchemaData)

trait Codec[A] {
  val codec: Results[Codecs[A]]
  val schema: Results[Schema]
}

object Codec {

  def apply[A](implicit schemaBuilder: SchemaBuilder[A],
               encoderBuilder: EncoderBuilder[A],
               decoderBuilder: DecoderBuilder[A]) =
    new Codec[A] {
      private val encoder = Encoder[A]
      private val decoder = Decoder[A]

      override val codec: Results[Codecs[A]] = encoder.data.map(e => Codecs(decoder, encoder, e.schemaData))
      override val schema: Results[Schema]   = encoder.data.map(_.schemaData.schema)
    }

  def schema[A](implicit codec: Codec[A]): Results[Schema] = codec.schema

  def encoder[A](implicit codec: Codec[A]): Results[Encoder[A]] = codec.codec.map(_.encoder)

  def decoder[A](implicit codec: Codec[A]): Results[Decoder[A]] = codec.codec.map(_.decoder)

  implicit class EncodeSyntax[A](val toEncode: A) extends AnyVal {
    def encode(implicit codec: Codec[A]) = codec.codec.flatMap(c => Encoder.encode(toEncode, c.encoder))
    def encodeAccumulating(implicit codec: Codec[A]) =
      codec.codec.flatMap(c => Encoder.encodeAccumulating(toEncode, c.encoder))
  }

  implicit class DecodeSyntax(val genericRecord: GenericRecord) extends AnyVal {
    def decode[A](implicit codec: Codec[A]) = codec.codec.flatMap(c => Decoder.decode(genericRecord, c.decoder))
    def decodeAccumulating[A](implicit codec: Codec[A]) =
      codec.codec.flatMap(c => Decoder.decodeAccumulating(genericRecord, c.decoder))
  }

  implicit class DecodeSyntaxGDR(val genericRecord: GenericData.Record) extends AnyVal {
    def decode[A](implicit codec: Codec[A]) = codec.codec.flatMap(c => Decoder.decode(genericRecord, c.decoder))
    def decodeAccumulating[A](implicit codec: Codec[A]) =
      codec.codec.flatMap(c => Decoder.decodeAccumulating(genericRecord, c.decoder))
  }

  final def toBinary[A](a: A)(implicit codec: Codec[A]): Results[Array[Byte]] =
    Codec.schema.flatMap { schema =>
      a.encode.flatMap { encoded =>
        safeL {
          val baos    = new ByteArrayOutputStream
          val encoder = EncoderFactory.get.binaryEncoder(baos, null)
          new GenericDatumWriter[Any](schema).write(encoded, encoder)
          encoder.flush()
          baos.toByteArray
        }
      }
    }

  final def fromBinary[A](bytes: Array[Byte])(implicit codec: Codec[A]): Results[A] =
    Codec.schema.flatMap { schema =>
      safeL {
        val bais                 = new ByteArrayInputStream(bytes)
        val decoder              = DecoderFactory.get.binaryDecoder(bais, null)
        val value: GenericRecord = new GenericDatumReader[GenericRecord](schema).read(null, decoder)
        value.decode
      }.flatMap(identity)
    }
}
