package com.rauchenberg.cupcatAvro.decoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.annotations.SchemaAnnotations._
import com.rauchenberg.cupcatAvro.common.{Error, Result}
import com.rauchenberg.cupcatAvro.decoder.helpers.ReflectionHelpers._
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import com.rauchenberg.cupcatAvro.decoder.helpers.SealedTraitUnionDispatcher._
import com.rauchenberg.cupcatAvro.decoder.helpers.{UnionEnum, UnionRecord}

import scala.reflect.runtime.universe._

trait Decoder[T] {

  def decodeFrom(fieldName: String, record: GenericRecord): Result[T]

}

object Decoder {

  def apply[T](implicit decoder: Decoder[T]) = decoder

  type Typeclass[T] = Decoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[T] =
      ctx.parameters.toList.traverse { param =>
        val annotations  = getAnnotations(param.annotations)
        val decodeResult = param.typeclass.decodeFrom(getName(annotations, param.label), record)
        (decodeResult, param.default) match {
          case (Left(_), Some(default)) => default.asRight
          case _                        => decodeResult
        }
      }.map(ctx.rawConstruct(_))
  }

  def dispatch[T : TypeTag](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[T] = {

      val enumErrorMsg = "couldn't instantiate ENUM for field"

      val recordValue = record.get(fieldName)
      val schema      = record.getSchema.getField(fieldName).schema()

      schema.getType match {
        case Schema.Type.UNION =>
          recordValue match {
            case container: GenericData.Record => dispatchUnionRecord(UnionRecord(container, fieldName, schema, ctx))
            case _                             => dispatchUnionEnum(UnionEnum(fieldName, record, ctx))
          }
        case Schema.Type.ENUM =>
          ctx.subtypes
            .filter(_.typeName.short == recordValue)
            .map(st => toCaseObject[T](st.typeName.full))
            .headOption
            .toRight(Error(s"$enumErrorMsg $fieldName"))
        case other => Error(s"Unsupported sealed trait type : $other, only UNION and ENUM are supported").asLeft
      }

    }

  }

}
