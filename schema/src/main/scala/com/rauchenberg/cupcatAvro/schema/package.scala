package com.rauchenberg.cupcatAvro

import cats.syntax.either._
import org.apache.avro.Schema

package object schema {

  type SchemaResult = Either[SchemaError, Schema]

  def safeSchema[T](f: => T): Either[SchemaError, T] = {
    Either.catchNonFatal(f).leftMap {
      e =>
        SchemaError(e.getMessage)
    }
  }


}
