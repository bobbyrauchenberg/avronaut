package com.rauchenberg.cupcatAvro.encoder.instances

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.encoder.Encoder
import org.apache.avro.Schema

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringEncoder = new Encoder[String] {
    override def encode(value: String, schema: Schema): Result[String] = value.asRight[Error]
  }

  implicit val booleanEncoder = new Encoder[Boolean] {
    override def encode(value: Boolean, schema: Schema): Result[Boolean] = value.asRight[Error]
  }

}
