package com.rauchenberg.avronaut

import cats.data.Reader
import com.rauchenberg.avronaut.common.Result
import com.rauchenberg.avronaut.schema.SchemaData
import org.apache.avro.generic.GenericRecord

package object decoder {

  implicit class LiftToReader[A](val a: Result[A]) extends AnyVal {
    def liftR: Reader[(GenericRecord, SchemaData), Result[A]] = Reader(_ => a)
  }

}
