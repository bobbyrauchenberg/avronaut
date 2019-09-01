package com.rauchenberg.cupcatAvro

import cats.syntax.either._

package object schema {

  def safeSchema[T](f: => T): Either[SchemaError, T] = {
    Either.catchNonFatal(f).leftMap {
      e =>
        SchemaError(e.getMessage)
    }
  }


}
