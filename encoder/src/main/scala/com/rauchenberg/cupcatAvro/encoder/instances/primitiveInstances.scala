package com.rauchenberg.cupcatAvro.encoder.instances

import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.encoder.Encoder
import com.rauchenberg.cupcatAvro.encoder.instances.utils.encodePrimitive
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringEncoder = new Encoder[String] {
    override def encode(value: String, schema: Schema): Result[String] = encodePrimitive(value, schema, STRING)
  }

  implicit val booleanEncoder = new Encoder[Boolean] {
    override def encode(value: Boolean, schema: Schema): Result[Boolean] = encodePrimitive(value, schema, BOOLEAN)
  }

  implicit val intEncoder = new Encoder[Int] {
    override def encode(value: Int, schema: Schema): Result[Int] = encodePrimitive(value, schema, INT)
  }

  implicit val longEncoder = new Encoder[Long] {
    override def encode(value: Long, schema: Schema): Result[Long] = encodePrimitive(value, schema, LONG)
  }

  implicit val floatEncoder = new Encoder[Float] {
    override def encode(value: Float, schema: Schema): Result[Float] = encodePrimitive(value, schema, FLOAT)
  }

  implicit val doubleEncoder = new Encoder[Double] {
    override def encode(value: Double, schema: Schema): Result[Double] = encodePrimitive(value, schema, DOUBLE)
  }

  implicit val bytesEncoder = new Encoder[Array[Byte]] {
    override def encode(value: Array[Byte], schema: Schema): Result[Array[Byte]] = encodePrimitive(value, schema, BYTES)
  }

}
