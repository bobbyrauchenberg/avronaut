package com.rauchenberg.cupcatAvro.encoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.Result
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.generic.{GenericData, GenericRecord}

trait Encoder[T] {

  def encode(value: T): Result[Any]

}

object Encoder {

  def apply[T](implicit encoder: Encoder[T]) = encoder

  type Typeclass[T] = Encoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T])(implicit avroSchema: AvroSchema[T]): Typeclass[T] = new Typeclass[T] {
    override def encode(value: T): Result[GenericRecord] = {
      ctx.parameters.map(p => p.typeclass.encode(p.dereference(value))).toList.sequence
        .flatMap { results =>
          avroSchema.schema.map { schema =>
            val record = new GenericData.Record(schema)
            results.zipWithIndex.foreach { case (r, i) => record.put(i, r) }
            record
          }
        }
    }
  }

}
