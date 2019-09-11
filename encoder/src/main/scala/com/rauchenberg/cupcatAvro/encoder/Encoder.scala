package com.rauchenberg.cupcatAvro.encoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.common.AvroError._
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
      val (errors, encoded) =
        (for {
          field <- schema.getFields.asScala
          param <- ctx.parameters.find(_.label == field.name)
        } yield param.typeclass.encode(param.dereference(value), field.schema)).toList.separate

      if (errors.isEmpty && encoded.nonEmpty) {
        val record = new GenericData.Record(schema)
        encoded.zipWithIndex.foreach { case (r, i) => record.put(i, r) }
        record.asRight
      } else {
        AggregatedError(Error(encoderErrorMsg(schema, value.toString)) +: errors).asLeft
      }
    }
  }

}
