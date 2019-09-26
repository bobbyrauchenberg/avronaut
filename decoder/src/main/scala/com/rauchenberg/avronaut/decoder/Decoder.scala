package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID
import cats.implicits._
import collection.JavaConverters._
import cats.implicits._
import com.rauchenberg.avronaut.common.{ReflectionHelpers, _}
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import shapeless.{:+:, CNil, Coproduct, Inr}

private[this] sealed trait DecodeOperation
private[this] final case class FullDecode(schema: Schema, genericRecord: GenericRecord)        extends DecodeOperation
private[this] final case class FieldDecode(schema: Schema.Field, genericRecord: GenericRecord) extends DecodeOperation
private[this] final case class TypeDecode(value: AvroType)                                     extends DecodeOperation

trait Decoder[A] {

  def apply(operation: DecodeOperation): Result[A]

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def decode[A](readerSchema: Schema, genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    decoder.apply(FullDecode(readerSchema, genericRecord))

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val params = ctx.parameters.toList

    override def apply(operation: DecodeOperation): Result[A] =
      (operation match {
        case FullDecode(schema, genericRecord) =>
          params.zip(fieldsFrom(schema)).traverse {
            case (param, field) =>
              val res = Parser
                .parse(FieldDecode(field, genericRecord))
                .map(TypeDecode(_))
                .flatMap(v => param.typeclass.apply(v))
              (res, param.default) match {
                case (Left(_), Some(default)) => default.asRight
                case _                        => res
              }
          }
        case TypeDecode(AvroRecord(fields)) =>
          params.zip(fields).traverse {
            case (param, avroType) =>
              typeclassOrDefault(TypeDecode(avroType), param.typeclass.apply, param.default)
          }
        case other =>
          params.traverse(param => typeclassOrDefault(other, param.typeclass.apply, param.default))
      }).map(ctx.rawConstruct(_))

    def typeclassOrDefault[B](value: DecodeOperation, f: DecodeOperation => Result[B], default: Option[B]) = {
      val res = f(value)
      (res, default) match {
        case (Left(_), Some(default)) => default.asRight
        case _                        => res
      }
    }

    private def fieldsFrom(s: Schema) = s.getFields.asScala.toList

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

  def error[A](expected: String, actual: A) = Error(s"expected $expected, got $actual").asLeft

  implicit val stringDecoder = new Decoder[String] {
    override def apply(value: DecodeOperation): Result[String] =
      value match {
        case TypeDecode(AvroString(v)) => v.asRight
        case _                         => error("string", value)
      }
  }

  implicit val booleanDecoder = new Decoder[Boolean] {
    override def apply(value: DecodeOperation): Result[Boolean] =
      value match {
        case TypeDecode(AvroBoolean(v)) => v.asRight
        case _                          => error("boolean", value)
      }
  }

  implicit val intDecoder = new Decoder[Int] {
    override def apply(value: DecodeOperation): Result[Int] =
      value match {
        case TypeDecode(AvroInt(v)) => v.asRight
        case _                      => error("int", value)
      }
  }

  implicit val longDecoder = new Decoder[Long] {
    override def apply(value: DecodeOperation): Result[Long] =
      value match {
        case TypeDecode(AvroLong(v)) => v.asRight
        case _                       => error("long", value)
      }
  }

  implicit val floatDecoder = new Decoder[Float] {
    override def apply(value: DecodeOperation): Result[Float] =
      value match {
        case TypeDecode(AvroFloat(v)) => v.asRight
        case _                        => error("float", value)
      }
  }

  implicit val doubleDecoder = new Decoder[Double] {
    override def apply(value: DecodeOperation): Result[Double] =
      value match {
        case TypeDecode(AvroDouble(v)) => v.asRight
        case _                         => error("double", value)
      }
  }

  implicit val bytesDecoder = new Decoder[Array[Byte]] {
    override def apply(value: DecodeOperation): Result[Array[Byte]] =
      value match {
        case TypeDecode(AvroBytes(v)) => v.asRight
        case _                        => error("Array[Byte]", value)
      }
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[List[A]] {
    override def apply(value: DecodeOperation): Result[List[A]] = value match {
      case TypeDecode(AvroArray(v)) =>
        v.traverse(at => elementDecoder.apply(TypeDecode(at)))
      case _ => error("list", value)
    }
  }

  implicit def seqDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Seq[A]] {
    override def apply(value: DecodeOperation): Result[Seq[A]] = listDecoder[A].apply(value)
  }

  implicit def vectorDecoder[A](implicit elementDecoder: Decoder[A]) = new Decoder[Vector[A]] {
    override def apply(value: DecodeOperation): Result[Vector[A]] = listDecoder[A].apply(value).map(_.toVector)
  }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]) = new Typeclass[Map[String, A]] {

    override def apply(value: DecodeOperation): Result[Map[String, A]] = value match {
      case TypeDecode(AvroMap(l)) =>
        l.traverse { entry =>
          elementDecoder(TypeDecode(entry.value)).map(entry.key -> _)
        }.map(_.toMap[String, A])
      case _ => error("map", value)
    }
  }

  implicit def offsetDateTimeDecoder = new Decoder[OffsetDateTime] {
    override def apply(value: DecodeOperation): Result[OffsetDateTime] = value match {
      case TypeDecode(AvroTimestampMillis(AvroLong(value))) =>
        safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC))
      case _ => error("OffsetDateTime / Long", value)
    }
  }

  implicit def instantDecoder = new Decoder[Instant] {
    override def apply(value: DecodeOperation): Result[Instant] = value match {
      case TypeDecode(AvroTimestampMillis(AvroLong(value))) => safe(Instant.ofEpochMilli(value))
      case _                                                => error("Instant / Long", value)
    }
  }

  implicit def uuidDecoder = new Decoder[UUID] {
    override def apply(value: DecodeOperation): Result[UUID] = value match {
      case TypeDecode(AvroUUID(AvroString(value))) => safe(java.util.UUID.fromString(value))
      case _                                       => error("UUID / String", value)
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]) = new Decoder[Option[A]] {
    override def apply(value: DecodeOperation): Result[Option[A]] =
      value match {
        case TypeDecode(AvroUnion(AvroNull)) => None.asRight
        case TypeDecode(AvroUnion(value))    => valueDecoder(TypeDecode(value)).map(Option(_))
        case other                           => valueDecoder(other).map(Option(_))
      }
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]) = new Decoder[Either[A, B]] {
    override def apply(value: DecodeOperation): Result[Either[A, B]] = {
      def runDecoders(value: DecodeOperation) =
        lDecoder(value).fold(_ => rDecoder(value).map(_.asRight), _.asLeft.asRight)
      value match {
        case TypeDecode(AvroUnion(AvroNull)) => runDecoders(value)
        case TypeDecode(AvroUnion(v))        => runDecoders(TypeDecode(v))
        case _                               => runDecoders(value)
      }
    }
  }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply(value: DecodeOperation): Result[CNil] = error("not to decode CNil", value)
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H], tDecoder: Decoder[T]) =
    new Decoder[H :+: T] {
      override def apply(value: DecodeOperation): Result[H :+: T] = value match {
        case TypeDecode(AvroUnion(v)) =>
          hDecoder(TypeDecode(v)) match {
            case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
            case _            => tDecoder(value).map(Inr(_))
          }
        case _ => tDecoder(value).map(Inr(_))
      }
    }

}
