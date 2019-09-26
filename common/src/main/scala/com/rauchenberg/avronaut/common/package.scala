package com.rauchenberg.avronaut

import cats.syntax.either._
import shapeless.ops.coproduct.Inject

import shapeless.Coproduct

package object common {

  type Result[A] = Either[Error, A]

  def safe[A](f: => A): Result[A] =
    Either.catchNonFatal(f).leftMap { e =>
      Error(e.getMessage)
    }

  implicit class CoproductOps[A](val t: A) extends AnyVal {
    def toCP[U <: Coproduct](implicit inj: Inject[U, A]): U = Coproduct[U](t)
  }

}
