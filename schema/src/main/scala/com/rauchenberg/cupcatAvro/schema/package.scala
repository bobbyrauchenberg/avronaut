package com.rauchenberg.cupcatAvro

import cats.syntax.either._
import org.apache.avro.Schema
import scala.collection.JavaConverters._

package object schema {

  type SchemaResult = Either[SchemaError, Schema]

  def schemaField[T](name: String, schema: Schema, doc: String) = safe(
    new Schema.Field(name, schema, doc)
  )

  def schemaField[T](name: String, schema: Schema, doc: String, default: T) = safe(
    new Schema.Field(name, schema, doc, default)
  )

  def schemaRecord[T](name: String, doc: String, namespace: String, isError: Boolean, fields: List[Schema.Field]) =
    safe(Schema.createRecord(name, doc, namespace, false, fields.asJava))

  def safe[T](f: => T): Either[SchemaError, T] = {
    Either.catchNonFatal(f).leftMap {
      e =>
        SchemaError(e.getMessage)
    }
  }


}
