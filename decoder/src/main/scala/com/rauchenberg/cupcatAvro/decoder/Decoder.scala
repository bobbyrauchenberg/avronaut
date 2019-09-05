package com.rauchenberg.cupcatAvro.decoder

import magnolia.{CaseClass, Magnolia}
import cats.implicits._
import org.apache.avro.generic.GenericRecord

trait Decoder[T] {

  def decodeFrom(value: String, record: GenericRecord): DecodeResult[T]

}

object Decoder {

  def apply[T](implicit decoder: Decoder[T]) = decoder

  type Typeclass[T] = Decoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    override def decodeFrom(fieldName: String, record: GenericRecord): DecodeResult[T] = {
      ctx.parameters.toList.traverse { param =>
        val decodeResult = param.typeclass.decodeFrom(param.label, record)
        (decodeResult, param.default) match {
          case (Left(_), Some(default)) => default.asRight
          case (res, _) => res
        }
      }.map(ctx.rawConstruct(_))
    }

  }

}



