package com.rauchenberg.cupcatAvro.encoder.instances

import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.encoder.{Encoded, Encoder, Primitive}
import com.rauchenberg.cupcatAvro.encoder.instances.utils.encodePrimitive
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringEncoder = new Encoder[String] {
    override def encode(value: String, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, STRING).map(Primitive(_))
  }

  implicit val booleanEncoder = new Encoder[Boolean] {
    override def encode(value: Boolean, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, BOOLEAN).map(Primitive(_))
  }

  implicit val intEncoder = new Encoder[Int] {
    override def encode(value: Int, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, INT).map(Primitive(_))
  }

  implicit val longEncoder = new Encoder[Long] {
    override def encode(value: Long, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, LONG).map(Primitive(_))
  }

  implicit val floatEncoder = new Encoder[Float] {
    override def encode(value: Float, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, FLOAT).map(Primitive(_))
  }

  implicit val doubleEncoder = new Encoder[Double] {
    override def encode(value: Double, schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, DOUBLE).map(Primitive(_))
  }

  implicit val bytesEncoder = new Encoder[Array[Byte]] {
    override def encode(value: Array[Byte], schema: Schema): Result[Encoded] =
      encodePrimitive(value, schema, BYTES).map(Primitive(_))
  }

}
