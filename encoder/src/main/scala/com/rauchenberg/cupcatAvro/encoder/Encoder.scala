package com.rauchenberg.cupcatAvro.encoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.Error.encoderErrorFor
import com.rauchenberg.cupcatAvro.common._
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

sealed trait Encoded
case class Primitive[T](primitive: T)                        extends Encoded
case class Record(genericRecord: GenericData.Record)         extends Encoded
case class Elements(elements: List[Encoded], schema: Schema) extends Encoded

trait Encoder[T] {

  def encode(value: T, schema: Schema): Result[Encoded]

}

object Encoder {

  def apply[T](implicit encoder: Encoder[T]) = encoder

  type Typeclass[T] = Encoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] =
    new Typeclass[T] {
      override def encode(value: T, schema: Schema): Result[Encoded] = {
        ((for {
            field <- schema.getFields.asScala
            param <- ctx.parameters.find(_.label == field.name)
          } yield param.typeclass.encode(param.dereference(value), field.schema))
          ).toList.sequence.map(l => Elements(l, schema))
      }.leftMap(_ => encoderErrorFor(schema, value.toString))
    }
}
