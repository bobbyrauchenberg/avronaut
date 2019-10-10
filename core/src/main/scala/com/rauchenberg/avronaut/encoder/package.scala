package com.rauchenberg.avronaut

import cats.data.Reader
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.schema.SchemaData

package object encoder {

  implicit class LiftToReader[A](val a: Result[A]) extends AnyVal {
    def liftR: Reader[SchemaData, Result[A]] = Reader(_ => a)
  }

}
