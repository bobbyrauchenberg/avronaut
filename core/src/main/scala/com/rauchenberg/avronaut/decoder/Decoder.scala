package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaData}
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.NoStackTrace

trait Decoder[A] {

  def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): A

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def apply[A](implicit decoder: Decoder[A]) = decoder

  def decode[A](genericRecord: GenericRecord, decoder: Decoder[A], schemaData: Result[SchemaData]) =
    schemaData.map { as =>
      decoder("", genericRecord, as)
    }

  case object StacklessException extends NoStackTrace

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    type Ret = Result[A]

    val params = ctx.parameters.toList

    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): A = {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      ctx.rawConstruct(params.flatMap { param =>
        val paramAnnotations = getAnnotations(param.annotations)
        val paramName        = paramAnnotations.name(param.label)

        schemaData.schemaMap.get(s"$namespace.$name").flatMap { schema =>
          schema.getFields.asScala.find(_.name == paramName).map { field =>
            valueOrDefault(
              safe(genericRecord.get(field.name) match {
                case gr: GenericRecord =>
                  param.typeclass.apply(gr, gr, schemaData)
                case _ =>
                  param.typeclass.apply(genericRecord.get(field.name), genericRecord, schemaData)
              }),
              param.default
            )
          }
        }
      })
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): A =
      ctx.subtypes
        .find(_.typeName.short == value.toString)
        .map(st => ReflectionHelpers.toCaseObject[A](st.typeName.full))
        .getOrElse(throw StacklessException)
  }

  private def valueOrDefault[B](value: B, default: Option[B]) =
    (value, default) match {
      case (Right(value), _)        => value
      case (Left(_), Some(default)) => default
      case other                    => other
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Error(s"expected $expected, got $actual").asLeft

  implicit val stringDecoder: Decoder[String] = new Decoder[String] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): String = value match {
      case s: String      => s
      case u: Utf8        => u.toString
      case a: Array[Byte] => new String(a)
    }
  }

  implicit val booleanDecoder: Decoder[Boolean] = new Decoder[Boolean] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Boolean =
      value match {
        case true  => true
        case false => false
      }
  }

  implicit val intDecoder: Decoder[Int] = new Decoder[Int] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Int = value.toString.toInt
  }

  implicit val longDecoder: Decoder[Long] = new Decoder[Long] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Long = value match {
      case l: Long => l
    }
  }

  implicit val floatDecoder: Decoder[Float] = new Decoder[Float] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Float = value match {
      case f: Float => f
    }

  }

  implicit val doubleDecoder: Decoder[Double] = new Decoder[Double] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Double = value match {
      case d: Double => d
    }
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = new Decoder[Array[Byte]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Array[Byte] =
      value.asInstanceOf[Array[Byte]]
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[List[A]] = new Typeclass[List[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): List[A] = {
      val list = value.asInstanceOf[java.util.List[A]]
      list.asScala.toList.map {
        case v =>
          v match {
            case gr: GenericRecord =>
              elementDecoder(gr, gr, schemaData)
            case _ => elementDecoder(v, genericRecord, schemaData)
          }
      }
    }
  }

  implicit def seqDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Seq[A]] = new Decoder[Seq[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Seq[A] =
      listDecoder[A](elementDecoder).apply(value, genericRecord, schemaData)
  }

  implicit def vectorDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Vector[A]] = new Decoder[Vector[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Vector[A] =
      listDecoder[A](elementDecoder).apply(value, genericRecord, schemaData).toVector
  }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] =
    new Decoder[Map[String, A]] {
      override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Map[String, A] =
        value
          .asInstanceOf[java.util.Map[String, A]]
          .asScala
          .map {
            case (k, v) =>
              v match {
                case gr: GenericRecord =>
                  k -> elementDecoder(gr, gr, schemaData)
                case _ => k -> elementDecoder(v, genericRecord, schemaData)
              }

          }
          .toMap
    }

  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = new Decoder[OffsetDateTime] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): OffsetDateTime =
      value match {
        case l: Long => OffsetDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC)
      }

  }

  implicit def instantDecoder: Decoder[Instant] = new Decoder[Instant] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Instant = value match {
      case l: Long => Instant.ofEpochMilli(l)
    }
  }

  implicit def uuidDecoder: Decoder[UUID] = new Decoder[UUID] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): UUID = value match {
      case s: String if (s != null) => java.util.UUID.fromString(s)
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Option[A] =
      if (value == null) None
      else
        Option(valueDecoder(value, genericRecord, schemaData))
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
    new Decoder[Either[A, B]] {
      override def apply[C](value: C, genericRecord: GenericRecord, schemaData: SchemaData): Either[A, B] =
        Try {
          rDecoder(value.asInstanceOf[A], genericRecord, schemaData).asRight
        }.toOption.fold[Either[A, B]](lDecoder(value.asInstanceOf[A], genericRecord, schemaData).asLeft)(v => v)
    }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): CNil =
      throw StacklessException
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = new Decoder[H :+: T] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): H :+: T =
      Try {
        val h = hDecoder(value, genericRecord, schemaData)
        Coproduct[H :+: T](h)
      }.toOption.fold[H :+: T](
        Inr(tDecoder(value, genericRecord, schemaData))
      )(v => v)
  }

}
