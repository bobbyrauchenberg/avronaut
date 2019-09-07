package com.rauchenberg.cupcatAvro.decoder.instances

import com.rauchenberg.cupcatAvro.decoder.Decoder
import org.apache.avro.generic.GenericRecord
import com.rauchenberg.cupcatAvro.decoder._
import com.rauchenberg.cupcatAvro.common._

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringDecoder = new Decoder[String] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[String] =
      safe(record.get(fieldName).toString)
  }

  implicit val booleanDecoder = new Decoder[Boolean] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Boolean] =
      safe(record.get(fieldName).asInstanceOf[Boolean])
  }

  implicit val intDecoder = new Decoder[Int] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Int] =
      safe(record.get(fieldName).asInstanceOf[Int])
  }

  implicit val longDecoder = new Decoder[Long] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Long] =
      safe(record.get(fieldName).asInstanceOf[Long])
  }

  implicit val floatDecoder = new Decoder[Float] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Float] =
      safe(record.get(fieldName).asInstanceOf[Float])
  }

  implicit val doubleDecoder = new Decoder[Double] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Double] =
      safe(record.get(fieldName).asInstanceOf[Double])
  }

  implicit val bytesDecoder = new Decoder[Array[Byte]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Array[Byte]] =
      safe(record.get(fieldName).asInstanceOf[Array[Byte]])
  }


}
