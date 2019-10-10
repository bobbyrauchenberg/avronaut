package com.rauchenberg.avronaut.schema

import shims._
import scalaz._
import Scalaz._
import cats.data.NonEmptyList
import com.rauchenberg.avronaut.common.recursion.recursion.Fix
import scalaz.{Applicative, Traverse}

sealed trait AvroSchemaADT
case object SchemaInt             extends AvroSchemaADT
case object SchemaLong            extends AvroSchemaADT
case object SchemaFloat           extends AvroSchemaADT
case object SchemaDouble          extends AvroSchemaADT
case object SchemaBoolean         extends AvroSchemaADT
case object SchemaString          extends AvroSchemaADT
case object SchemaNull            extends AvroSchemaADT
case object SchemaBytes           extends AvroSchemaADT
case object SchemaUUID            extends AvroSchemaADT
case object SchemaTimestampMillis extends AvroSchemaADT
case class SchemaEnum(name: String, namespace: String, doc: Option[String], values: NonEmptyList[String])
    extends AvroSchemaADT
case class SchemaList(value: AvroSchemaADT)                     extends AvroSchemaADT
case class SchemaMap(value: AvroSchemaADT)                      extends AvroSchemaADT
case class SchemaOption(value: AvroSchemaADT)                   extends AvroSchemaADT
case class SchemaCoproduct(values: NonEmptyList[AvroSchemaADT]) extends AvroSchemaADT
case class SchemaNamedField[B](name: String, doc: Option[String], default: Option[B], value: AvroSchemaADT)
    extends AvroSchemaADT
case class SchemaRecord(name: String, namespace: String, doc: Option[String], values: List[AvroSchemaADT])
    extends AvroSchemaADT

sealed trait AvroSchemaF[+A]
case object SchemaIntF             extends AvroSchemaF[Nothing]
case object SchemaLongF            extends AvroSchemaF[Nothing]
case object SchemaFloatF           extends AvroSchemaF[Nothing]
case object SchemaDoubleF          extends AvroSchemaF[Nothing]
case object SchemaBooleanF         extends AvroSchemaF[Nothing]
case object SchemaStringF          extends AvroSchemaF[Nothing]
case object SchemaNullF            extends AvroSchemaF[Nothing]
case object SchemaBytesF           extends AvroSchemaF[Nothing]
case object SchemaUUIDF            extends AvroSchemaF[Nothing]
case object SchemaTimestampMillisF extends AvroSchemaF[Nothing]
case class SchemaEnumF(name: String, namespace: String, doc: Option[String], values: NonEmptyList[String])
    extends AvroSchemaF[Nothing]
case class SchemaListF[A](value: A)                     extends AvroSchemaF[A]
case class SchemaMapF[A](value: A)                      extends AvroSchemaF[A]
case class SchemaOptionF[A](value: A)                   extends AvroSchemaF[A]
case class SchemaCoproductF[A](values: NonEmptyList[A]) extends AvroSchemaF[A]
case class SchemaNamedFieldF[A, B](name: String, doc: Option[String], default: Option[B], value: A)
    extends AvroSchemaF[A]
case class SchemaRecordF[A](name: String, namespace: String, doc: Option[String], values: List[A])
    extends AvroSchemaF[A]

object AvroSchemaF {

  type AvroSchemaFix = Fix[AvroSchemaF]

  def apply(f: AvroSchemaF[AvroSchemaFix]): AvroSchemaFix = Fix(f)

  implicit val avroSchemaTraverse = new Traverse[AvroSchemaF] {
    override def traverseImpl[G[_], A, B](fa: AvroSchemaF[A])(f: A => G[B])(
        implicit G: Applicative[G]): G[AvroSchemaF[B]] =
      fa match {
        case SchemaIntF                  => G.pure(SchemaIntF)
        case SchemaLongF                 => G.pure(SchemaLongF)
        case SchemaDoubleF               => G.pure(SchemaDoubleF)
        case SchemaFloatF                => G.pure(SchemaFloatF)
        case SchemaBooleanF              => G.pure(SchemaBooleanF)
        case SchemaStringF               => G.pure(SchemaStringF)
        case SchemaNullF                 => G.pure(SchemaNullF)
        case SchemaBytesF                => G.pure(SchemaBytesF)
        case SchemaUUIDF                 => G.pure(SchemaUUIDF)
        case s @ SchemaEnumF(_, _, _, _) => G.pure(s)
        case SchemaTimestampMillisF      => G.pure(SchemaTimestampMillisF)
        case SchemaListF(value)          => G.map(f(value))(SchemaListF(_))
        case SchemaMapF(value)           => G.map(f(value))(SchemaMapF(_))
        case SchemaOptionF(value)        => G.map(f(value))(SchemaOptionF(_))
        case SchemaCoproductF(values)    => G.map(values.traverse(f))(SchemaCoproductF(_))
        case SchemaNamedFieldF(name, default, doc, value) =>
          G.map(f(value))(SchemaNamedFieldF(name, default, doc, _))
        case SchemaRecordF(name, namespace, doc, values) =>
          G.map(values.traverse(f))(SchemaRecordF(name, namespace, doc, _))
      }

    override def map[A, B](fa: AvroSchemaF[A])(f: A => B): AvroSchemaF[B] = super.map(fa)(f)
  }
}
