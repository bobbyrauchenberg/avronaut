package com.rauchenberg.cupcatAvro.encoder.instances

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.encoder.Encoder

object primitiveInstances extends primitiveInstances

trait primitiveInstances {

  implicit val stringEncoder = new Encoder[String] {
    override def encode(value: String): Result[String] = value.asRight[Error]
  }

  implicit val booleanEncoder = new Encoder[Boolean] {
    override def encode(value: Boolean): Result[Boolean] = value.asRight[Error]
  }

}
