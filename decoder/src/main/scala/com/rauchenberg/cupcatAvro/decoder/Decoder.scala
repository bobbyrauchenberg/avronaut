package com.rauchenberg.cupcatAvro.decoder

import magnolia.{CaseClass, Magnolia}
import cats.implicits._
import org.apache.avro.generic.GenericRecord

trait Decoder[T] {

  def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[T]

}

object Decoder {

  def apply[T](implicit decoder: Decoder[T]) = decoder

  type Typeclass[T] = Decoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[T] = {
      val applied = ctx.parameters.toList.traverse { param =>
        record.get(param.label)
        param.typeclass.decodeFrom(param.label, record)
      }
      applied.map(ctx.rawConstruct(_))
    }

  }

}



