package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.{
  safe,
  AvroArray,
  AvroBoolean,
  AvroBytes,
  AvroDouble,
  AvroEnum,
  AvroField,
  AvroFloat,
  AvroInt,
  AvroLong,
  AvroMap,
  AvroNull,
  AvroString,
  AvroTimestampMillis,
  AvroType,
  AvroUUID,
  AvroUnion,
  Error,
  ParseFail,
  Result
}
import com.rauchenberg.avronaut.decoder.helpers.ReflectionHelpers
import magnolia.{CaseClass, Magnolia, SealedTrait}
import shapeless.{:+:, CNil, Coproduct, Inr}

trait Decoder[A] {

  def apply(avroType: AvroType): Result[A]

}

object Decoder {

  def apply[A](implicit decoder: Decoder[A]) = decoder

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(avroType: AvroType): Result[A] =
      ctx.parameters.toList.traverse { param =>
        def runDecoder(at: AvroType) = param.typeclass.apply(at)

        def errorFor(fieldName: String) =
          Error(s"ran out of records traversing ${ctx.typeName.full}, searching for $fieldName in $avroType").asLeft

        val fieldName   = s"${ctx.typeName.full}.${param.label}"
        val nextRecords = avroType.findAllByKey[A](fieldName) // should this be at?

        val decodeResult = nextRecords.headOption match {
          case Some(ParseFail(_, _)) => Error("got a ParseFail, will try for a default").asLeft
          case Some(AvroField(_, v)) => runDecoder(v)
          case Some(other)           => runDecoder(other)
          case None                  => errorFor(fieldName)
          case _                     => Error("couldn't find an AvroType to decode, will try for a default").asLeft
        }
        (decodeResult, param.default) match {
          case (Left(_), Some(default)) => default.asRight
          case _                        => decodeResult
        }
      }.map(ctx.rawConstruct(_))
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(avroType: AvroType): Result[A] =
      avroType match {
        case AvroEnum(_, value) =>
          ctx.subtypes
            .find(_.typeName.short == value.toString)
            .map(st => safe(ReflectionHelpers.toCaseObject[A](st.typeName.full)))
            .getOrElse(Error(s"wasn't able to find or to instantiate enum value $value in $avroType").asLeft[A])
        case _ => Error("not an enum").asLeft
      }
  }

  implicit val stringDecoder = new Decoder[String] {
    override def apply(avroType: AvroType): Result[String] =
      avroType match {
        case AvroString(v) => v.asRight
        case other         => Error(s"expected a string, got $other").asLeft
      }
  }

  implicit val booleanDecoder = new Decoder[Boolean] {
    override def apply(avroType: AvroType): Result[Boolean] =
      avroType match {
        case AvroBoolean(v) => v.asRight
        case _              => Error("expected a bool").asLeft
      }

  }

  implicit val intDecoder = new Decoder[Int] {
    override def apply(avroType: AvroType): Result[Int] =
      avroType match {
        case AvroInt(v) => v.asRight
        case _          => Error(s"int decoder expected an int, got $avroType").asLeft
      }
  }

  implicit val longDecoder = new Decoder[Long] {
    override def apply(avroType: AvroType): Result[Long] =
      avroType match {
        case AvroLong(v) => v.asRight
        case _           => Error("expected a long").asLeft
      }
  }

  implicit val floatDecoder = new Decoder[Float] {
    override def apply(avroType: AvroType): Result[Float] =
      avroType match {
        case AvroFloat(v) => v.asRight
        case _            => Error("expected a float").asLeft
      }
  }

  implicit val doubleDecoder = new Decoder[Double] {
    override def apply(avroType: AvroType): Result[Double] =
      avroType match {
        case AvroDouble(v) => v.asRight
        case _             => Error(s"double decoder expected a double, got $avroType").asLeft
      }
  }

  implicit val bytesDecoder = new Decoder[Array[Byte]] {
    override def apply(avroType: AvroType): Result[Array[Byte]] =
      avroType match {
        case AvroBytes(v) => v.asRight
        case _            => Error("bytes decoder expected an Array[Byte]").asLeft
      }
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[List[A]] {
    override def apply(avroType: AvroType): Result[List[A]] = avroType match {
      case AvroArray(_, v) =>
        v.traverse(elementDecoder.apply(_))
      case other => Error(s"list decoder expected to get a list, got $other").asLeft
    }
  }

  implicit def seqDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Seq[A]] {
    override def apply(avroType: AvroType): Result[Seq[A]] = listDecoder[A].apply(avroType)
  }

  implicit def vectorDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Vector[A]] {
    override def apply(avroType: AvroType): Result[Vector[A]] = listDecoder[A].apply(avroType).map(_.toVector)
  }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]) = new Typeclass[Map[String, A]] {

    override def apply(avroType: AvroType): Result[Map[String, A]] = avroType match {
      case AvroMap(_, l) =>
        l.traverse { entry =>
          elementDecoder(entry.value).map(entry.key -> _)
        }.map(_.toMap[String, A])
      case _ => Error(s"expected an AvroMap, got $avroType").asLeft
    }
  }

  implicit def offsetDateTimeDecoder = new Decoder[OffsetDateTime] {
    override def apply(avroType: AvroType): Result[OffsetDateTime] = avroType match {
      case AvroTimestampMillis(_, AvroLong(value)) =>
        safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC))
      case _ => Error(s"OffsetDateTime decoder expected an AvroLong, got $avroType").asLeft
    }
  }

  implicit def instantDecoder = new Decoder[Instant] {
    override def apply(avroType: AvroType): Result[Instant] = avroType match {
      case AvroTimestampMillis(_, AvroLong(value)) => safe(Instant.ofEpochMilli(value))
      case _                                       => Error(s"OffsetDateTime decoder expected an AvroLong, got $avroType").asLeft
    }
  }

  implicit def uuidDecoder = new Decoder[UUID] {
    override def apply(at: AvroType): Result[UUID] = at match {
      case AvroUUID(_, AvroString(value)) => safe(java.util.UUID.fromString(value))
      case _                              => Error(s"UUID decoder expected an AvroUUID, got $at").asLeft
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]) = new Decoder[Option[A]] {
    override def apply(at: AvroType): Result[Option[A]] =
      at match {
        case AvroUnion(_, AvroNull) => None.asRight
        case AvroUnion(_, value)    => valueDecoder(value).map(Option(_))
        case other                  => valueDecoder(other).map(Option(_))
      }
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]) = new Decoder[Either[A, B]] {
    override def apply(at: AvroType): Result[Either[A, B]] = {
      def runDecoders(value: AvroType) = lDecoder(value).fold(_ => rDecoder(value).map(_.asRight), _.asLeft.asRight)
      at match {
        case AvroUnion(_, value) if (!value.isNull) => runDecoders(value)
        case other                                  => runDecoders(other)
      }
    }
  }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply(at: AvroType): Result[CNil] = Error("decoded CNil").asLeft
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H], tDecoder: Decoder[T]) =
    new Decoder[H :+: T] {
      override def apply(at: AvroType): Result[H :+: T] = at match {
        case AvroUnion(_, value) =>
          hDecoder(value) match {
            case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
            case _            => tDecoder(at).map(Inr(_))
          }
        case _ => tDecoder(at).map(Inr(_))
      }
    }
}
