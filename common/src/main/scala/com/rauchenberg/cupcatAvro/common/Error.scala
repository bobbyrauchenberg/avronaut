package com.rauchenberg.cupcatAvro.common

import org.apache.avro.Schema

case class Error(msg: String)

object Error {

  def encoderErrorFor(schema: Schema, value: String) = Error(s"Invalid schema: $schema, for value: $value")

}
