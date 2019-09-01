package com.rauchenberg.cupcatAvro.schema

import cats.implicits._
import org.apache.avro.Schema
import magnolia.{CaseClass, Magnolia}
import scala.collection.JavaConverters._

trait AvroSchema[T] {

  def schema: AvroSchema.SchemaResult

}

object AvroSchema {

  type SchemaResult = Either[SchemaError, Schema]

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](cc: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {
      cc.parameters.toList.traverse { p =>
        p.typeclass.schema.flatMap { v =>
          p.default.traverse { default =>
            safeSchema(new Schema.Field(p.label, v, "", default))
          }.map(_.getOrElse(new Schema.Field(p.label, v)))
        }
      }.map { fields =>
        Schema.createRecord(fields.asJava)
      }
    }
  }
}
