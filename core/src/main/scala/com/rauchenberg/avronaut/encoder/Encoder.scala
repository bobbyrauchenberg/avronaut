package com.rauchenberg.avronaut.encoder

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericData
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

import scala.collection.JavaConverters._

trait Encoder[@specialized A] {

  def avroType: AvroType = Ignore
  def apply(a: A, sd: SchemaData): Avro

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def encode[A](a: A)(implicit encoder: Encoder[A], schema: Result[SchemaData]): Either[Error, GenericData.Record] =
    for {
      schemaData <- schema
      encoded    <- encoder.apply(a, schemaData).asRight
      genRec <- encoded match {
                 case AvroRecord(schema, values) =>
                   Parser(new GenericData.Record(schemaData.schema)).parse(AvroRoot(schema, values))
                 case _ => Error(s"Can only encode records, got $encoded").asLeft
               }
    } yield genRec

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] =
    new Typeclass[A] {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      override def apply(value: A, schemaData: SchemaData): Avro =
        schemaData.schemaMap.get(s"$namespace.$name") match {
          case None => AvroError(s"found no schema for type $namespace.$name")
          case Some(schema) =>
            AvroRecord(
              schema,
              schema.getFields.asScala.toList.map { field =>
                ctx.parameters.toList
                  .find(_.label == field.name)
                  .map { p =>
                    p.typeclass.apply(p.dereference(value), schemaData)
                  }
                  .getOrElse(AvroError("boom!"))
              }
            )
        }

      override def avroType: AvroType = AvroRecord
    }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Encoder[A] {
    override def apply(value: A, schemaData: SchemaData): Avro = {
      if (ctx.subtypes.isEmpty) true else false //just here to keep compiler flags happy until i use this
      val res: Avro = AvroEnum(value.toString)
      res
    }

    override def avroType: AvroType = AvroSealed
  }

  implicit val stringEncoder: Encoder[String] = new Encoder[String] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: String, sd: SchemaData): Avro = AvroString(value)
  }

  implicit val booleanEncoder: Encoder[Boolean] = new Typeclass[Boolean] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Boolean, sd: SchemaData): Avro = AvroBoolean(value)
  }

  implicit val intEncoder: Encoder[Int] = new Encoder[Int] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Int, sd: SchemaData): Avro = AvroInt(value)
  }

  implicit val longEncoder: Encoder[Long] = new Encoder[Long] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Long, sd: SchemaData): Avro = AvroLong(value)
  }

  implicit val floatEncoder: Encoder[Float] = new Encoder[Float] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Float, sd: SchemaData): Avro = AvroFloat(value)
  }

  implicit val doubleEncoder: Encoder[Double] = new Encoder[Double] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Double, sd: SchemaData): Avro = AvroDouble(value)
  }

  implicit val bytesEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
    override def avroType: AvroType = AvroPrimitive

    override def apply(value: Array[Byte], sd: SchemaData): Avro = AvroBytes(value)
  }

  implicit def listEncoder[@specialized A](implicit elementEncoder: Encoder[A]) = new Encoder[List[A]] {
    override def apply(value: List[A], sd: SchemaData): Avro =
      elementEncoder.avroType match {
        case AvroPrimitive => {
          AvroPrimitiveArray[A](value.asJava) // this is safe as scala doesn't allow nulls for non-string primitives
        }
        case _ => AvroArray(value.map(v => elementEncoder(v, sd)))
      }
  }

  implicit def optionEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    override def apply(value: Option[A], sd: SchemaData): Avro =
      AvroUnion(value.fold[Avro](AvroNull)(v => elementEncoder.apply(v, sd)))
  }

  implicit def mapEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Map[String, A]] =
    new Encoder[Map[String, A]] {
      override def apply(value: Map[String, A], sd: SchemaData): Avro =
        AvroMap(value.toList.map {
          case (k, v) =>
            (k -> elementEncoder(v, sd)) // needs to be optimized
        })
    }

  implicit def eitherEncoder[A, B](implicit lEncoder: Encoder[A], rEncoder: Encoder[B]): Encoder[Either[A, B]] =
    new Encoder[Either[A, B]] {
      override def apply(value: Either[A, B], sd: SchemaData): Avro = value.fold(lEncoder(_, sd), rEncoder(_, sd))
    }

  implicit def cnilEncoder: Encoder[CNil] = new Encoder[CNil] {
    override def apply(value: CNil, sd: SchemaData): Avro = AvroError("encoding CNil should never happen")
  }

  implicit def coproductEncoder[H, T <: Coproduct](implicit hEncoder: Encoder[H],
                                                   tEncoder: Encoder[T]): Encoder[H :+: T] = new Encoder[H :+: T] {
    override def apply(value: H :+: T, sd: SchemaData): Avro = value match {
      case Inl(h) => hEncoder(h, sd)
      case Inr(v) => tEncoder(v, sd)
    }
  }

  implicit val uuidEncoder: Encoder[UUID] = new Encoder[UUID] {
    override def apply(value: UUID, sd: SchemaData): Avro =
      AvroLogical(AvroString(value.toString)) // do we need this extra AvroLogical wrapper?
  }

  implicit val dateTimeEncoder: Encoder[OffsetDateTime] = new Encoder[OffsetDateTime] {
    override def apply(value: OffsetDateTime, sd: SchemaData): Avro =
      AvroLogical(AvroLong(value.toInstant.toEpochMilli)) // do we need this extra AvroLogical wrapper?
  }

  implicit val instantEncoder = new Encoder[Instant] {
    override def apply(value: Instant, sd: SchemaData): Avro = AvroLogical(AvroLong(value.toEpochMilli))
  }

}
