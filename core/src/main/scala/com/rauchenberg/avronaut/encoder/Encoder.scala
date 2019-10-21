package com.rauchenberg.avronaut.encoder

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.{Error, Result}
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaData}
import magnolia.{CaseClass, Magnolia}
import org.apache.avro.generic.{GenericData, GenericRecord}
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

trait Encoder[A] {

  type Ret

  def apply(value: A, schemaData: SchemaData): Ret

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Encoder[A] = macro Magnolia.gen[A]

  def encode[A](a: A)(implicit encoder: Encoder[A], schema: AvroSchema[A]): Either[Error, GenericRecord] = {
    println("a : " + a)
    schema.data.flatMap { schema =>
      val res = encoder.apply(a, schema)
      println("res : " + res)
      res match {
        case Right(gr: GenericData.Record) => gr.asInstanceOf[GenericRecord].asRight[Error]
        case _                             => Error("should have got a GenericData.Record from encoder").asLeft
      }
    }
  }

  def combine[A](ctx: CaseClass[Typeclass, A]): Encoder[A] =
    new Encoder[A] {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      type Ret = Result[GenericRecord]

      override def apply(value: A, sd: SchemaData): Result[GenericRecord] =
        sd.schemaMap.get(s"$namespace.$name") match {
          case None => throw new RuntimeException("fuck")
          case Some(schema) =>
            val gr = new GenericData.Record(schema)
            schema.getFields.asScala.toList.zipWithIndex.traverse {
              case (field, i) =>
                ctx.parameters.toList
                  .find(_.label == field.name)
                  .map { param =>
                    val paramValue = param.dereference(value)
                    param.typeclass.apply(paramValue, sd) match {
                      case Left(v)      => gr.put(i, v).asRight[Error]
                      case Right(v)     => gr.put(i, v).asRight[Error]
                      case e @ Error(_) => e.asLeft[Unit]
                      case other        => gr.put(i, other).asRight[Error]
                    }
                  }
                  .getOrElse(Error("couldn't find the typeclass for param matching : " + s"$namespace.$name").asLeft)
            }.map(_ => gr)
        }

    }

  implicit val stringEncoder: Encoder[String] = new Encoder[String] {
    type Ret = String
    override def apply(value: String, schemaData: SchemaData): String = value
  }

  implicit val boolEncoder: Encoder[Boolean] = new Encoder[Boolean] {
    type Ret = Boolean
    override def apply(value: Boolean, schemaData: SchemaData): Boolean = value
  }

  implicit val intEncoder: Encoder[Int] = new Encoder[Int] {
    type Ret = Int
    override def apply(value: Int, schemaData: SchemaData): Int = value
  }

  implicit val floatEncoder: Encoder[Float] = new Encoder[Float] {
    type Ret = Float
    override def apply(value: Float, schemaData: SchemaData): Float = value
  }

  implicit val doubleEncoder: Encoder[Double] = new Encoder[Double] {
    type Ret = Double
    override def apply(value: Double, schemaData: SchemaData): Double = value
  }

  implicit val longEncoder: Encoder[Long] = new Encoder[Long] {
    type Ret = Long
    override def apply(value: Long, schemaData: SchemaData): Long = value
  }

  implicit def mapEncoder[A](implicit aEncoder: Encoder[A]): Encoder[Map[String, A]] = new Encoder[Map[String, A]] {
    override type Ret = java.util.Map[String, aEncoder.Ret]
    override def apply(value: Map[String, A], schemaData: SchemaData): Ret =
      value.map {
        case (k, v) =>
          k -> aEncoder(v, schemaData)
      }.asJava
  }

  implicit def listEncoder[A : ClassTag](implicit aEncoder: Encoder[A]): Encoder[List[A]] = new Encoder[List[A]] {

    type Ret = java.util.List[aEncoder.Ret]

    override def apply(value: List[A], schemaData: SchemaData): Ret =
      value.map { v =>
        aEncoder(v, schemaData)
      }.asJava
  }

  implicit def optionEncoder[A](implicit aEncoder: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {

    type Ret = Either[Null, aEncoder.Ret]

    override def apply(value: Option[A], schemaData: SchemaData): Ret =
      value match {
        case None    => Left(null)
        case Some(v) => aEncoder(v, schemaData).asRight
      }
  }

  implicit def eitherEncoder[A, B](implicit aEncoder: Encoder[A], bEncoder: Encoder[B]) =
    new Encoder[Either[A, B]] {
      override type Ret = Either[aEncoder.Ret, bEncoder.Ret]

      override def apply(value: Either[A, B], schemaData: SchemaData): Ret =
        value.fold(aEncoder(_, schemaData).asLeft, bEncoder(_, schemaData).asRight)
    }

  implicit val cnilEncoder: Encoder[CNil] = new Encoder[CNil] {
    override type Ret = Error

    override def apply(value: CNil, schemaData: SchemaData): Error = Error("should never get a CNil")
  }

  implicit def coproductEncoder[A, B <: Coproduct](implicit aEncoder: Encoder[A],
                                                   bEncoder: Encoder[B]): Encoder[A :+: B] = new Encoder[A :+: B] {
    override type Ret = Either[aEncoder.Ret, bEncoder.Ret]

    override def apply(value: A :+: B, schemaData: SchemaData): Ret =
      value match {
        case Inl(a) => aEncoder(a, schemaData).asLeft[bEncoder.Ret]
        case Inr(b) => bEncoder(b, schemaData).asRight[aEncoder.Ret]
      }
  }

  implicit val uuidEncoder: Encoder[UUID] = new Encoder[UUID] {
    override type Ret = String
    override def apply(value: UUID, schemaData: SchemaData): String = value.toString
  }

  implicit val instantEncoder: Encoder[Instant] = new Encoder[Instant] {
    override type Ret = Long
    override def apply(value: Instant, schemaData: SchemaData): Long = value.toEpochMilli
  }

  implicit val dateTimeEncoder: Encoder[OffsetDateTime] = new Encoder[OffsetDateTime] {
    override type Ret = Long
    override def apply(value: OffsetDateTime, schemaData: SchemaData): Long = value.toInstant.toEpochMilli
  }

}
