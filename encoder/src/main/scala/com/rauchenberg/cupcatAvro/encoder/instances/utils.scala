package com.rauchenberg.cupcatAvro.encoder.instances

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common.Error.encoderErrorFor
import com.rauchenberg.cupcatAvro.common.Result
import org.apache.avro.Schema
import org.apache.avro.Schema.Type

object utils {

  def encodePrimitive[T](value: T, schema: Schema, expectedType: Type): Result[T] = {
    if (schema.getType != expectedType) encoderErrorFor(schema, value.toString).asLeft
    else value.asRight
  }

}
