package com.rauchenberg.avronaut.schema

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import com.rauchenberg.avronaut.common.ReflectionHelpers.isEnum
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations._
import com.rauchenberg.avronaut.common.{Error, Results}
import magnolia.{CaseClass, Magnolia, SealedTrait}
import shapeless.{:+:, CNil, Coproduct}

import scala.reflect.runtime.universe._

trait SchemaBuilder[A] {
  def schema: Results[AvroSchemaADT]
}

object SchemaBuilder {

  def apply[A](implicit avroSchema: SchemaBuilder[A]) = avroSchema

  def toSchema[A](implicit avroSchema: SchemaBuilder[A]): Results[SchemaData] =
    avroSchema.schema.flatMap(Parser(_).parse)

  type Typeclass[A] = SchemaBuilder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val annotations       = getAnnotations(ctx.annotations)
    val doc               = annotations.doc
    val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

    override def schema: Results[AvroSchemaADT] =
      ctx.parameters.toList.traverse { param =>
        val paramAnnotations = getAnnotations(param.annotations)
        val paramName        = paramAnnotations.name(param.label)
        val sch              = param.typeclass.schema
        val ns               = paramAnnotations.flatMap(_.values.get(Namespace))

        (sch, ns) match {
          case (Right(s @ SchemaRecord(_, _, _, _)), Some(ns)) =>
            SchemaNamedField(paramName, paramAnnotations.doc, param.default, s.copy(namespace = ns)).asRight
          case (Right(s @ SchemaEnum(_, _, _, _, _)), Some(ns)) =>
            SchemaNamedField(paramName, paramAnnotations.doc, param.default, s.copy(namespace = ns)).asRight
          case (Right(so @ SchemaOption(s @ SchemaRecord(_, _, _, _))), Some(ns)) =>
            SchemaNamedField(paramName, paramAnnotations.doc, param.default, SchemaOption(s.copy(namespace = ns))).asRight
          case (other, _) => other.map(SchemaNamedField(paramName, paramAnnotations.doc, param.default, _))
        }
      }.map(SchemaRecord(name, namespace, doc, _))
  }

  def dispatch[A : WeakTypeTag](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val anno              = getAnnotations(ctx.annotations)
    val lastDot           = ctx.typeName.full.lastIndexOf(".")
    val fullName          = ctx.typeName.full.substring(0, lastDot)
    val (name, namespace) = getNameAndNamespace(anno, ctx.typeName.short, fullName)

    val subtypes = ctx.subtypes.toList.toNel

    override def schema: Results[AvroSchemaADT] =
      subtypes.fold[Results[AvroSchemaADT]](List(Error("Got an empty list of symbols building Enum or Union")).asLeft)(
        st =>
          if (isEnum)
            SchemaEnum(name, namespace, anno.doc, st.map(_.typeName.short), st.map(_.typeName.full)).asRight
          else {
            st.traverse(_.typeclass.schema).map(SchemaCoproduct(_))
        })

  }

  implicit val intSchema = new SchemaBuilder[Int] {
    override def schema: Results[AvroSchemaADT] = SchemaInt.asRight
  }

  implicit val longSchema = new SchemaBuilder[Long] {
    override def schema: Results[AvroSchemaADT] = SchemaLong.asRight
  }

  implicit val floatSchema = new SchemaBuilder[Float] {
    override def schema: Results[AvroSchemaADT] = SchemaFloat.asRight
  }

  implicit val doubleSchema = new SchemaBuilder[Double] {
    override def schema: Results[AvroSchemaADT] = SchemaDouble.asRight
  }

  implicit val stringSchema = new SchemaBuilder[String] {
    override def schema: Results[AvroSchemaADT] = SchemaString.asRight
  }

  implicit val booleanSchema = new SchemaBuilder[Boolean] {
    override def schema: Results[AvroSchemaADT] = SchemaBoolean.asRight
  }

  implicit val bytesSchema = new SchemaBuilder[Array[Byte]] {
    override def schema: Results[AvroSchemaADT] = SchemaBytes.asRight
  }

  implicit val nullSchema = new SchemaBuilder[Null] {
    override def schema: Results[AvroSchemaADT] = SchemaNull.asRight
  }

  implicit def listSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[List[A]] {
    override def schema: Results[AvroSchemaADT] = elementSchema.schema.map(SchemaList(_))
  }

  implicit def setSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[Set[A]] {
    override def schema: Results[AvroSchemaADT] = listSchema[A].schema
  }

  implicit def seqSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[Seq[A]] {
    override def schema: Results[AvroSchemaADT] = elementSchema.schema.map(SchemaList(_))
  }

  implicit def vectorSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[Vector[A]] {
    override def schema: Results[AvroSchemaADT] = elementSchema.schema.map(SchemaList(_))
  }

  implicit def mapSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[Map[String, A]] {
    override def schema: Results[AvroSchemaADT] = elementSchema.schema.map(SchemaMap(_))
  }

  implicit def optionSchema[A](implicit elementSchema: SchemaBuilder[A]) = new SchemaBuilder[Option[A]] {
    override def schema: Results[AvroSchemaADT] =
      elementSchema.schema.map(SchemaOption(_))
  }

  implicit def eitherSchema[A, B](implicit lElementSchema: SchemaBuilder[A], rElementSchema: SchemaBuilder[B]) =
    new SchemaBuilder[Either[A, B]] {
      override def schema: Results[AvroSchemaADT] = lElementSchema.schema.map2(rElementSchema.schema) {
        case (r, l) =>
          SchemaCoproduct(NonEmptyList.of(r, l))
      }
    }

  implicit def uuidSchema[A] = new SchemaBuilder[UUID] {
    override def schema: Results[AvroSchemaADT] = SchemaUUID.asRight
  }

  implicit def offsetDateTimeSchema[A] = new SchemaBuilder[OffsetDateTime] {
    override def schema: Results[AvroSchemaADT] = SchemaTimestampMillis.asRight
  }

  implicit def instantDateTimeSchema[A] = new SchemaBuilder[Instant] {
    override def schema: Results[AvroSchemaADT] = SchemaTimestampMillis.asRight
  }

  implicit def cnilSchema[H](implicit hSchema: SchemaBuilder[H]) = new SchemaBuilder[H :+: CNil] {
    override def schema: Results[AvroSchemaADT] = hSchema.schema.map(v => SchemaCoproduct(NonEmptyList.of(v)))

  }

  implicit def coproductSchema[H, T <: Coproduct](implicit hSchema: SchemaBuilder[H], tSchema: SchemaBuilder[T]) =
    new SchemaBuilder[H :+: T] {
      override def schema: Results[AvroSchemaADT] =
        hSchema.schema
          .map2(tSchema.schema) {
            case (h, t) =>
              SchemaCoproduct(NonEmptyList.of(h, t))
          }
    }
}
