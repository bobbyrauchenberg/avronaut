package com.rauchenberg.cupcatAvro.common

import org.apache.avro.Schema

sealed trait AvroError extends Product with Serializable

object AvroError {

  def encoderErrorMsg(schema: Schema, value: String) = s"Invalid schema: $schema, for value: $value"

}

case class Error(msg: String) extends AvroError
case class AggregatedError(errors: List[AvroError]) extends AvroError
