package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats._
import cats.data.Reader
import cats.implicits._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.common.{ReflectionHelpers, _}
import com.rauchenberg.avronaut.decoder.Parser.parse
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaData}
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericRecord
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.collection.JavaConverters._

trait Decoder[A] {

  def apply(value: Avro): Reader[(GenericRecord, SchemaData), Result[A]]

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def apply[A](implicit decoder: Decoder[A]) = decoder

  def decode[A](genericRecord: GenericRecord)(implicit decoder: Decoder[A], avroSchema: AvroSchema[A]) =
    avroSchema.data.flatMap { as =>
      decoder(AvroDecode).apply((genericRecord, as))
    }

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val params = ctx.parameters.toList

    override def apply(value: Avro): Reader[(GenericRecord, SchemaData), Result[A]] = {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      Reader[(GenericRecord, SchemaData), Result[A]] {
        case (genericRecord, schemaData) =>
          (value match {
            case AvroDecode =>
              params.flatTraverse { param =>
                val paramAnnotations = getAnnotations(param.annotations)
                val paramName        = paramAnnotations.name(param.label)

                schemaData.schemaMap.get(s"$namespace.$name").toList.flatTraverse { schema =>
                  schema.getFields.asScala.toList.filter(_.name == paramName).traverse { field =>
                    valueOrDefault(
                      parse(field, genericRecord).flatMap(v => param.typeclass.apply(v)((genericRecord, schemaData))),
                      param.default)
                  }
                }
              }
            case AvroRecord(_, fields) =>
              params.zip(fields).traverse {
                case (param, avroType) =>
                  valueOrDefault(param.typeclass.apply(avroType)((genericRecord, schemaData)), param.default)
              }
            case other =>
              params.traverse(param =>
                valueOrDefault(param.typeclass.apply(other)((genericRecord, schemaData)), param.default))
          }).map(ctx.rawConstruct(_))
      }
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def apply(value: Avro): Reader[(GenericRecord, SchemaData), Result[A]] =
      value match {
        case AvroEnum(v) =>
          ctx.subtypes
            .find(_.typeName.short == v.toString)
            .map(st => safe(ReflectionHelpers.toCaseObject[A](st.typeName.full)))
            .getOrElse(Error(s"wasn't able to find or to instantiate enum value $v in $v").asLeft[A])
            .liftR
        case _ => Error("not an enum").asLeft.liftR
      }
  }

  private def valueOrDefault[B](value: Result[B], default: Option[B]) =
    (value, default) match {
      case (Left(_), Some(default)) => default.asRight
      case _                        => value
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Error(s"expected $expected, got $actual").asLeft

  implicit val stringDecoder: Decoder[String] = {
    case AvroString(v) => v.asRight[Error].liftR
    case value         => error("string", value).liftR
  }

  implicit val booleanDecoder: Decoder[Boolean] = {
    case AvroBoolean(v) => v.asRight[Error].liftR
    case value          => error("boolean", value).liftR
  }

  implicit val intDecoder: Decoder[Int] = {
    case AvroInt(v) => v.asRight[Error].liftR
    case value      => error("int", value).liftR
  }

  implicit val longDecoder: Decoder[Long] = {
    case AvroLong(v) => v.asRight[Error].liftR
    case value       => error("long", value).liftR
  }

  implicit val floatDecoder: Decoder[Float] = {
    case AvroFloat(v) => v.asRight[Error].liftR
    case value        => error("float", value).liftR
  }

  implicit val doubleDecoder: Decoder[Double] = {
    case AvroDouble(v) => v.asRight[Error].liftR
    case value         => error("double", value).liftR
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = {
    case AvroBytes(v) => v.asRight[Error].liftR
    case value        => error("Array[Byte]", value).liftR
  }

  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[List[A]] = {
    case AvroArray(v) =>
      v.traverse(at => elementDecoder.apply(at)).map(_.sequence)
    case value => error("list", value).liftR
  }

  implicit def seqDecoder[A : Decoder]: Decoder[Seq[A]] = listDecoder[A].apply(_).map(_.map(_.toSeq))

  implicit def vectorDecoder[A : Decoder]: Decoder[Vector[A]] = listDecoder[A].apply(_).map(_.map(_.toVector))

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] = {
    case AvroMap(l) =>
      Reader(m => l.traverse { case (k, v) => elementDecoder(v).apply(m).map(k -> _) }.map(_.toMap))
    case value => error("map", value).liftR
  }

  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = {
    case AvroLogical(AvroLong(value)) =>
      safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)).liftR
    case value => error("OffsetDateTime / Long", value).liftR
  }

  implicit def instantDecoder: Decoder[Instant] = {
    case AvroLogical(AvroLong(value)) => safe(Instant.ofEpochMilli(value)).liftR
    case value                        => error("Instant / Long", value).liftR
  }

  implicit def uuidDecoder: Decoder[UUID] = {
    case AvroLogical(AvroString(value)) => safe(java.util.UUID.fromString(value)).liftR
    case value                          => error("UUID / String", value).liftR
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = {
    case AvroUnion(AvroNull) => none[A].asRight[Error].liftR
    case AvroUnion(value)    => valueDecoder(value).map(_.map(Option(_)))
    case other               => valueDecoder(other).map(_.map(Option(_)))
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
    value => {
      def runDecoders(value: Avro) =
        lDecoder(value).flatMap {
          _.fold(
            _ => rDecoder(value).map(_.map(_.asRight[A])),
            _.asLeft[B].asRight[Error].liftR
          )
        }
      value match {
        case AvroUnion(AvroNull) => runDecoders(value)
        case AvroUnion(v)        => runDecoders(v)
        case _                   => runDecoders(value)
      }
    }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply(value: Avro) =
      Reader { _ =>
        Error(s"ended up reaching CNil when decoding $value").asLeft
      }
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = {
    case value @ AvroUnion(v) =>
      Reader { m =>
        hDecoder(v).apply(m) match {
          case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
          case _            => tDecoder(value).apply(m).map(Inr(_))
        }
      }
    case value => Reader(m => tDecoder(value).apply(m).map(Inr(_)))
  }

}
