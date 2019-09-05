package com.rauchenberg.cupcatAvro

import cats.syntax.either._
package object decoder {

  type DecodeResult[T] = Either[DecodeError, T]

  def safe[T](f: => T): Either[DecodeError, T] = {
    Either.catchNonFatal(f).leftMap {
      e =>
        DecodeError(e.getMessage)
    }
  }

}
