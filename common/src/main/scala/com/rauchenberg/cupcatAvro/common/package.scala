package com.rauchenberg.cupcatAvro

import cats.syntax.either._
import shapeless.ops.coproduct.Inject

import shapeless.Coproduct

package object common {

  type Result[T] = Either[Error, T]

  def safe[T](f: => T): Result[T] =
    Either.catchNonFatal(f).leftMap { e =>
      Error(e.getMessage)
    }
  def safe[T](f: => T): Result[T] = {
    Either.catchNonFatal(f).leftMap {
      e =>
        Error(e.getMessage)
    }
  }

  implicit class CoproductOps[T](val t: T) extends AnyVal {
    def toCP[U <: Coproduct](implicit inj: Inject[U, T]): U = Coproduct[U](t)
  }

}
