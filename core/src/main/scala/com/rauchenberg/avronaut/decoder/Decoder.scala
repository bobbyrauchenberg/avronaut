package com.rauchenberg.avronaut.decoder

import java.nio.ByteBuffer

import cats.implicits._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaData}
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8

import scala.collection.JavaConverters._

trait Decoder[A] {

  def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): A

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def apply[A](implicit decoder: Decoder[A]) = decoder

  def decode[A](genericRecord: GenericRecord)(implicit decoder: Decoder[A], avroSchema: AvroSchema[A]) =
    avroSchema.data.map { as =>
      decoder("", genericRecord, as)
    }

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    type Ret = Result[A]

    val params = ctx.parameters.toList

    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): A = {

      println("in apply gen rec: " + genericRecord)
      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      ctx.rawConstruct(params.flatMap { param =>
        val paramAnnotations = getAnnotations(param.annotations)
        val paramName        = paramAnnotations.name(param.label)

        schemaData.schemaMap.get(s"$namespace.$name").toList.flatMap { schema =>
          schema.getFields.asScala.toList.filter(_.name == paramName).map { field =>
            valueOrDefault(
              safe(genericRecord.get(field.name) match {
                case gr: GenericRecord => param.typeclass.apply(gr, gr, schemaData)
                case _                 => param.typeclass.apply(genericRecord.get(field.name), genericRecord, schemaData)
              }),
              param.default
            )
          }
        }
      })
    }
  }

//  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): A = {
//      null
//      value match {
//        case AvroEnum(v) =>
//          ctx.subtypes
//            .find(_.typeName.short == v.toString)
//            .map(st => safe(ReflectionHelpers.toCaseObject[A](st.typeName.full)))
//            .getOrElse(Error(s"wasn't able to find or to instantiate enum value $v in $v").asLeft[A])
//        case _ => Error("not an enum").asLeft
//      }
//    }
//  }

  private def valueOrDefault[B](value: B, default: Option[B]) =
    (value, default) match {
      case (Left(_), Some(default)) => default
      case (Right(value), _)        => value
      case other                    => other
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Error(s"expected $expected, got $actual").asLeft

  implicit val stringDecoder: Decoder[String] = new Decoder[String] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): String = value match {
      case u: Utf8          => u.toString
      case s: String        => s
      case cs: CharSequence => cs.toString
      case bb: ByteBuffer   => new String(bb.array)
      case a: Array[Byte]   => new String(a)
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
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Int = value match {
      case byte: Byte   => byte.toInt
      case short: Short => short.toInt
      case int: Int     => int
    }
  }

  implicit val longDecoder: Decoder[Long] = new Decoder[Long] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Long =
      value.asInstanceOf[Long]
  }

  implicit val floatDecoder: Decoder[Float] = new Decoder[Float] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Float =
      value.asInstanceOf[Float]
  }

  implicit val doubleDecoder: Decoder[Double] = new Decoder[Double] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Double =
      value.asInstanceOf[Double]
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = new Decoder[Array[Byte]] {
    override def apply[B](value: B, genericRecord: GenericRecord, schemaData: SchemaData): Array[Byte] =
      value.asInstanceOf[Array[Byte]]
  }

//  implicit def listDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[List[A]] = new Typeclass[List[A]] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[List[A]] = {
//      val list = genericRecord.get(fieldName).asInstanceOf[List[A]]
//      list.map(v => elementDecoder(v))
//    }
//      value match {
//        case AvroArray(v) =>
//          v.traverse(at => elementDecoder.apply(at, genericRecord, schemaData))
//        case value => error("list", value)
//      }
//  }
//
//  implicit def seqDecoder[A : Decoder]: Decoder[Seq[A]] = listDecoder[A].apply(_, _, _)
//
//  implicit def vectorDecoder[A : Decoder]: Decoder[Vector[A]] = listDecoder[A].apply(_, _, _).map(_.toVector)
//
//  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] =
//    new Decoder[Map[String, A]] {
//      override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[Map[String, A]] =
//        value match {
//          case AvroMap(l) =>
//            l.traverse { case (k, v) => elementDecoder(v, genericRecord, schemaData).map(k -> _) }.map(_.toMap)
//          case value => error("map", value)
//        }
//    }
//
//  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = new Decoder[OffsetDateTime] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[OffsetDateTime] =
//      value match {
//        case AvroLogical(AvroLong(value)) =>
//          safe(OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC))
//        case value => error("OffsetDateTime / Long", value)
//      }
//  }
//
//  implicit def instantDecoder: Decoder[Instant] = new Decoder[Instant] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[Instant] =
//      value match {
//        case AvroLogical(AvroLong(value)) => safe(Instant.ofEpochMilli(value))
//        case value                        => error("Instant / Long", value)
//      }
//  }
//
//  implicit def uuidDecoder: Decoder[UUID] = new Typeclass[UUID] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[UUID] =
//      value match {
//        case AvroLogical(AvroString(value)) => safe(java.util.UUID.fromString(value))
//        case value                          => error("UUID / String", value)
//      }
//  }
//
//  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[Option[A]] =
//      value match {
//        case AvroUnion(AvroNull) => none[A].asRight[Error]
//        case AvroUnion(value)    => valueDecoder(value, genericRecord, schemaData).map(Option(_))
//        case other               => valueDecoder(other, genericRecord, schemaData).map(Option(_))
//      }
//  }
//
//  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
//    new Typeclass[Either[A, B]] {
//      override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[Either[A, B]] = {
//        def runDecoders(fieldName: String) =
//          lDecoder(value, genericRecord, schemaData).fold(
//            _ => rDecoder(value, genericRecord, schemaData).map(_.asRight[A]),
//            _.asLeft[B].asRight[Error]
//          )
//        value match {
//          case AvroUnion(AvroNull) => runDecoders(value)
//          case AvroUnion(v)        => runDecoders(v)
//          case _                   => runDecoders(value)
//        }
//      }
//    }
//
//  implicit object CNilDecoderValue extends Decoder[CNil] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData) =
//      Error(s"ended up reaching CNil when decoding $value").asLeft
//  }
//
//  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
//                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = new Decoder[H :+: T] {
//    override def apply(fieldName: String, genericRecord: GenericRecord, schemaData: SchemaData): Result[H :+: T] =
//      value match {
//        case value @ AvroUnion(v) =>
//          hDecoder(v, genericRecord, schemaData) match {
//            case r @ Right(_) => r.map(h => Coproduct[H :+: T](h))
//            case _            => tDecoder(value, genericRecord, schemaData).map(Inr(_))
//          }
//        case value => tDecoder(value, genericRecord, schemaData).map(Inr(_))
//      }
//  }

}
