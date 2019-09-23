package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.{
  AvroArray,
  AvroBoolean,
  AvroBytes,
  AvroDouble,
  AvroEnum,
  AvroField,
  AvroFloat,
  AvroInt,
  AvroLong,
  AvroNull,
  AvroString,
  AvroTimestampMillis,
  AvroType,
  AvroUUID,
  AvroUnion,
  Error,
  Result
}
import magnolia.{CaseClass, Magnolia, SealedTrait}

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

        val fieldName   = s"${ctx.typeName.full}.${param.label}"
        val nextRecords = avroType.findAllByKey[A](fieldName) // should this be at?

        val decodeResult = nextRecords.headOption match {
          case Some(AvroField(_, v)) => runDecoder(v)
          case Some(other)           => runDecoder(other)
          case None                  => Error(s"ran out of records finding: $fieldName in $avroType").asLeft
        }
        (decodeResult, param.default) match {
          case (Left(_), Some(default)) => default.asRight
          case _                        => decodeResult
        }
      }.map(ctx.rawConstruct(_))
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def apply(avroType: AvroType): Result[T] =
      avroType match {
        case AvroEnum(_, value) =>
          val classNameToFind = value.getClass.getSimpleName
          val toFind =
            (if (classNameToFind.endsWith("$")) classNameToFind.dropRight(1) else classNameToFind).toLowerCase
          val find = ctx.subtypes.find(_.typeName.short.toLowerCase == toFind)
          find
            .map(_ => value.asInstanceOf[T].asRight[Error])
            .getOrElse(Error(s"couldn't find enum value $value in $avroType").asLeft[T])
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

  implicit def listDecoder[T](implicit elementDecoder: Decoder[T]) = new Decoder[List[T]] {
    override def apply(avroType: AvroType): Result[List[T]] = avroType match {
      case AvroArray(_, v) =>
        v.traverse(elementDecoder.apply(_))
      case other => Error(s"list decoder expected to get a list, got $other").asLeft
    }
  }

  implicit def seqDecoder[T](implicit elementDecoder: Decoder[T]) = new Decoder[Seq[T]] {
    override def apply(avroType: AvroType): Result[Seq[T]] = listDecoder[T].apply(avroType)
  }

  implicit def vectorDecoder[T](implicit elementDecoder: Decoder[T]) = new Decoder[Vector[T]] {
    override def apply(avroType: AvroType): Result[Vector[T]] = listDecoder[T].apply(avroType).map(_.toVector)
  }

  implicit def offsetDateTimeDecoder = new Decoder[OffsetDateTime] {
    override def apply(avroType: AvroType): Result[OffsetDateTime] = avroType match {
      case AvroTimestampMillis(_, AvroLong(value)) =>
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC).asRight
      case _ => Error(s"OffsetDateTime decoder expected an AvroLong, got $avroType").asLeft
    }
  }

  implicit def uuidDecoder = new Decoder[UUID] {
    override def apply(avroType: AvroType): Result[UUID] = avroType match {
      case AvroUUID(_, v) => v.asRight
      case _              => Error(s"UUID decoder expected an AvroUUID, got $avroType").asLeft
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
}
