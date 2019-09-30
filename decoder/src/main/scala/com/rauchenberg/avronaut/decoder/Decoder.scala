package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.{ReflectionHelpers, _}
import com.rauchenberg.avronaut.decoder.Parser.parse
import com.rauchenberg.avronaut.schema.AvroSchema
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericRecord
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.collection.JavaConverters._

private[this] sealed trait DecodeOperation
private[this] final case class FullDecode(genericRecord: GenericRecord) extends DecodeOperation
private[this] final case class TypeDecode(value: AvroType)              extends DecodeOperation

trait Decoder[A] {

  def apply(operation: DecodeOperation): Result[A]

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def decode[A](genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    decoder.apply(FullDecode(genericRecord))

  def combine[A](ctx: CaseClass[Typeclass, A])(implicit s: AvroSchema[A]): Typeclass[A] = new Typeclass[A] {

    val params = ctx.parameters.toList

    override def apply(operation: DecodeOperation): Result[A] =
      (operation match {
        case FullDecode(genericRecord) =>
          s.schema.flatMap { schema =>
            schema.getFields.asScala.toList.traverse { field =>
              ctx.parameters.toList
                .find(_.label == field.name)
                .map(_.asRight)
                .getOrElse(Error(s"couldn't find param for schema field ${field.name}").asLeft)
                .flatMap { param =>
                  valueOrDefault(parse(field, genericRecord).flatMap(v => param.typeclass.apply(TypeDecode(v))),
                                 param.default)
                }
            }
          }
        case TypeDecode(AvroRecord(fields)) =>
          params.zip(fields).traverse {
            case (param, avroType) =>
              valueOrDefault(param.typeclass.apply(TypeDecode(avroType)), param.default)
          }
        case other =>
          params.traverse(param => valueOrDefault(param.typeclass.apply(other), param.default))
      }).map(ctx.rawConstruct(_))

  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(value: DecodeOperation): Result[A] =
      value match {
        case TypeDecode(AvroEnum(v)) =>
          ctx.subtypes
            .find(_.typeName.short == v.toString)
            .map(st => safe(ReflectionHelpers.toCaseObject[A](st.typeName.full)))
            .getOrElse(Error(s"wasn't able to find or to instantiate enum value $v in $v").asLeft[A])
        case _ => Error("not an enum").asLeft
      }
  }

  private def valueOrDefault[B](value: Result[B], default: Option[B]) =
    (value, default) match {
      case (Left(_), Some(default)) => default.asRight
      case _                        => value
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Error(s"expected $expected, got $actual").asLeft

  implicit val stringDecoder: Decoder[String] = {
    case TypeDecode(AvroString(v)) => v.asRight
    case value                     => error("string", value)
  }

  implicit val booleanDecoder: Decoder[Boolean] = {
    case TypeDecode(AvroBoolean(v)) => v.asRight
    case value                      => error("boolean", value)
  }

  implicit val intDecoder: Decoder[Int] = {
    case TypeDecode(AvroNumber(v)) => v.toInt
    case value                     => error("int", value)
  }

  implicit val longDecoder: Decoder[Long] = {
    case TypeDecode(AvroNumber(v)) => v.toLong
    case value                     => error("long", value)
  }

  implicit val floatDecoder: Decoder[Float] = {
    case TypeDecode(AvroNumber(v)) => v.toFloat.asRight
    case value                     => error("float", value)
  }

  implicit val doubleDecoder: Decoder[Double] = {
    case TypeDecode(AvroNumber(v)) => v.toDouble.asRight
    case value                     => error("double", value)
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = {
    case TypeDecode(AvroBytes(v)) => v.asRight
    case value                    => error("Array[Byte]", value)
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[List[A]] = {
    case TypeDecode(AvroArray(v)) =>
      v.traverse(at => elementDecoder.apply(TypeDecode(at)))
    case value => error("list", value)
  }

  implicit def seqDecoder[A : Decoder]: Decoder[Seq[A]] = listDecoder[A].apply(_)

  implicit def vectorDecoder[A : Decoder]: Decoder[Vector[A]] = listDecoder[A].apply(_).map(_.toVector)

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] = {
    case TypeDecode(AvroMap(l)) =>
      l.traverse { entry =>
        elementDecoder(TypeDecode(entry.value)).map(entry.key -> _)
      }.map(_.toMap[String, A])
    case value => error("map", value)
  }

  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = {
    case TypeDecode(AvroLogicalType(AvroNumber(AvroNumLong(value)))) =>
      safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC))
    case value => error("OffsetDateTime / Long", value)
  }

  implicit def instantDecoder: Decoder[Instant] = {
    case TypeDecode(AvroLogicalType(AvroNumber(AvroNumLong(value)))) => safe(Instant.ofEpochMilli(value))
    case value                                                       => error("Instant / Long", value)
  }

  implicit def uuidDecoder: Decoder[UUID] = {
    case TypeDecode(AvroLogicalType(AvroString(value))) => safe(java.util.UUID.fromString(value))
    case value                                          => error("UUID / String", value)
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = {
    case TypeDecode(AvroUnion(AvroNull)) => None.asRight
    case TypeDecode(AvroUnion(value))    => valueDecoder(TypeDecode(value)).map(Option(_))
    case other                           => valueDecoder(other).map(Option(_))
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
    value => {
      def runDecoders(value: DecodeOperation) =
        lDecoder(value).fold(_ => rDecoder(value).map(_.asRight), _.asLeft.asRight)
      value match {
        case TypeDecode(AvroUnion(AvroNull)) => runDecoders(value)
        case TypeDecode(AvroUnion(v))        => runDecoders(TypeDecode(v))
        case _                               => runDecoders(value)
      }
    }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply(value: DecodeOperation): Result[CNil] = error("not to decode CNil", value)
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = {
    case value @ TypeDecode(AvroUnion(v)) =>
      hDecoder(TypeDecode(v)) match {
        case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
        case _            => tDecoder(value).map(Inr(_))
      }
    case value => tDecoder(value).map(Inr(_))
  }

}
