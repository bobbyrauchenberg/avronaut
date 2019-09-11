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

}
