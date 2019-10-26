package com.rauchenberg.avronaut.encoder

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.common.{Error, Result}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia, SealedTrait}
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

  def encode[A](a: A, encoder: Encoder[A], schemaData: Result[SchemaData]): Either[Error, GenericRecord] =
    schemaData.flatMap { schema =>
      val res = encoder.apply(a, schema)
      res match {
        case gr: GenericData.Record =>
          gr.asRight[Error]
        case _ => Error("should have got a GenericData.Record from encoder").asLeft
      }
    }

  def combine[A](ctx: CaseClass[Typeclass, A]): Encoder[A] =
    new Encoder[A] {

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      //should be Result[GenericRecord] or M[GenericRecord]
      type Ret = GenericRecord

      override def apply(value: A, sd: SchemaData): GenericRecord =
        sd.schemaMap.get(s"$namespace.$name") match {
          case None => throw new RuntimeException("fuck")
          case Some(schema) =>
            val gr = new GenericData.Record(schema)
            schema.getFields.asScala.toList.zipWithIndex.foreach {
              case (field, i) =>
                ctx.parameters.toList
                  .find(_.label == field.name)
                  .map { param =>
                    val paramValue = param.dereference(value)
                    param.typeclass.apply(paramValue, sd) match {
                      case Left(v)  => gr.put(i, v)
                      case Right(v) => gr.put(i, v)
                      case Error(_) => ()
                      case other    => gr.put(i, other)
                    }
                  }
                  .getOrElse(())
            }
            gr
        }
    }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Encoder[A] {
    println(ctx)
    override type Ret = String

    override def apply(value: A, schemaData: SchemaData): String =
      value.toString
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

  implicit val bytesEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
    override type Ret = Array[Byte]

    override def apply(value: Array[Byte], schemaData: SchemaData): Array[Byte] = value
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

    type Ret = Any

    override def apply(value: Option[A], schemaData: SchemaData): Ret =
      value.fold[Any](null)(v => aEncoder(v, schemaData))
  }

  implicit def eitherEncoder[A, B](implicit aEncoder: Encoder[A], bEncoder: Encoder[B]) =
    new Encoder[Either[A, B]] {
      override type Ret = Any

      override def apply(value: Either[A, B], schemaData: SchemaData): Ret =
        value.fold(aEncoder(_, schemaData), bEncoder(_, schemaData))
    }

  implicit val cnilEncoder: Encoder[CNil] = new Encoder[CNil] {
    override type Ret = Error

    override def apply(value: CNil, schemaData: SchemaData): Error = Error("should never get a CNil")
  }

  implicit def coproductEncoder[A, B <: Coproduct](implicit aEncoder: Encoder[A],
                                                   bEncoder: Encoder[B]): Encoder[A :+: B] = new Encoder[A :+: B] {
    override type Ret = Any

    override def apply(value: A :+: B, schemaData: SchemaData): Ret =
      value match {
        case Inl(a) => aEncoder(a, schemaData)
        case Inr(b) => bEncoder(b, schemaData)
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
