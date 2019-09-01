package com.rauchenberg.cupcat1.schema

import cats.implicits._
import org.apache.avro.Schema
import magnolia.{CaseClass, Magnolia}
import scala.collection.JavaConverters._

trait AvroSchema[T] {

  type SchemaResult = Either[SchemaError, Schema]

  def schema: SchemaResult

}

object AvroSchema {
  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](cc: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {
      cc.parameters.toList.traverse { p =>
        p.typeclass.schema.map { v =>
          new Schema.Field(p.label, v)
        }
      }.map { fields =>
        Schema.createRecord(fields.asJava)
      }
    }
  }
}
