package com.rauchenberg.cupcatAvro.encoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.{Error, Result}
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}

import scala.collection.JavaConverters._

trait Encoder[T] {

  def encode(value: T, schema: Schema): Result[Any]

}

object Encoder {

  def apply[T](implicit encoder: Encoder[T]) = encoder

  type Typeclass[T] = Encoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def encode(value: T, schema: Schema): Result[GenericRecord] = {
      val encoded =
        for {
          field <- schema.getFields.asScala
          param <- ctx.parameters.find(_.label == field.name)
        } yield param.typeclass.encode(param.dereference(value), field.schema)

      encoded.toList match {
        case Nil =>
          Error(s"Invalid schema: $schema, for value: $value").asLeft
        case enc =>
          enc.sequence.map { results =>
            val record = new GenericData.Record(schema)
            results.zipWithIndex.foreach { case (r, i) => record.put(i, r) }
            record
          }
      }
    }
  }

}
