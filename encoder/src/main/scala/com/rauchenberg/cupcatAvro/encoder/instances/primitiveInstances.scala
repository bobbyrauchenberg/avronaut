package com.rauchenberg.cupcatAvro.encoder.instances

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.common.AvroError._
import com.rauchenberg.cupcatAvro.encoder.Encoder
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringEncoder = new Encoder[String] {
    override def encode(value: String, schema: Schema): Result[String] = {
      if (schema.getType != STRING) Error(encoderErrorMsg(schema, value)).asLeft
      else value.asRight
    }
  }

  implicit val booleanEncoder = new Encoder[Boolean] {
    override def encode(value: Boolean, schema: Schema): Result[Boolean] = {
      if (schema.getType != BOOLEAN) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

  implicit val intEncoder = new Encoder[Int] {
    override def encode(value: Int, schema: Schema): Result[Int] = {
      if (schema.getType != INT) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

  implicit val longEncoder = new Encoder[Long] {
    override def encode(value: Long, schema: Schema): Result[Long] = {
      if (schema.getType != LONG) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

  implicit val floatEncoder = new Encoder[Float] {
    override def encode(value: Float, schema: Schema): Result[Float] = {
      if (schema.getType != FLOAT) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

  implicit val doubleEncoder = new Encoder[Double] {
    override def encode(value: Double, schema: Schema): Result[Double] = {
      if (schema.getType != DOUBLE) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

  implicit val bytesEncoder = new Encoder[Array[Byte]] {
    override def encode(value: Array[Byte], schema: Schema): Result[Array[Byte]] = {
      if (schema.getType != BYTES) Error(encoderErrorMsg(schema, value.toString)).asLeft
      else value.asRight
    }
  }

}
