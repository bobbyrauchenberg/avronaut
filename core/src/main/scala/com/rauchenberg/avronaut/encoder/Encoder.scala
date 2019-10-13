package com.rauchenberg.avronaut.encoder

import cats.data.Reader
import cats.implicits._
import com.rauchenberg.avronaut.common.Avro._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericData
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

import scala.collection.JavaConverters._

trait Encoder[A] {

  def apply(value: A): Reader[SchemaData, Result[Avro]]

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def encode[A](a: A, schemaData: SchemaData)(implicit encoder: Encoder[A]): Either[Error, GenericData.Record] =
    for {
      encoded <- encoder.apply(a)(schemaData)
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

      override def apply(value: A): Reader[SchemaData, Result[Avro]] =
        Reader[SchemaData, Result[Avro]] { schemaData =>
          schemaData.schemaMap.get(s"$namespace.$name") match {
            case None => Error(s"found no schema for type $namespace.$name").asLeft
            case Some(schema) =>
              val record = AvroRecord(schema, _)
              schema.getFields.asScala.toList.traverse { field =>
                ctx.parameters.toList
                  .find(_.label == field.name)
                  .map(_.asRight)
                  .getOrElse(Error(s"couldn't find param for schema field ${field.name}").asLeft)
                  .flatMap { param =>
                    param.typeclass.apply(param.dereference(value)).apply(schemaData)
                  }
              }.map(record)
          }
        }
    }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Encoder[A] {
    override def apply(value: A): Reader[SchemaData, Result[Avro]] = {
      println(ctx)
      val res: Result[Avro] = toAvroEnum(value.toString)
      res.liftR
    }
  }

  implicit val stringEncoder: Encoder[String] = (value: String) => toAvroString(value).liftR

  implicit val booleanEncoder: Encoder[Boolean] = (value: Boolean) => toAvroBoolean(value).liftR

  implicit val intEncoder: Encoder[Int] = (value: Int) => toAvroInt(value).liftR

  implicit val longEncoder: Encoder[Long] = (value: Long) => toAvroLong(value).liftR

  implicit val floatEncoder: Encoder[Float] = (value: Float) => toAvroFloat(value).liftR

  implicit val doubleEncoder: Encoder[Double] = (value: Double) => toAvroDouble(value).liftR

  implicit val bytesEncoder: Encoder[Array[Byte]] = (value: Array[Byte]) => toAvroBytes(value).liftR

  implicit def listEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[List[A]] =
    (value: List[A]) => value.traverse(elementEncoder.apply(_)).map(_.sequence.map(AvroArray(_)))

  implicit def optionEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Option[A]] =
    (value: Option[A]) =>
      Reader { sd =>
        value.fold[Result[Avro]](toAvroNull(null))(v => elementEncoder.apply(v).apply(sd)).map(AvroUnion(_))
    }

  implicit def mapEncoder[A](implicit elementEncoder: Encoder[A]): Encoder[Map[String, A]] =
    (value: Map[String, A]) =>
      Reader { sd =>
        value.toList.traverse {
          case (k, v) =>
            elementEncoder.apply(v)(sd).map((k -> _))
        }.map(AvroMap(_))
    }

  implicit def eitherEncoder[A, B](implicit lEncoder: Encoder[A], rEncoder: Encoder[B]): Encoder[Either[A, B]] =
    (value: Either[A, B]) => value.fold(lEncoder.apply, rEncoder.apply)

  implicit def cnilEncoder: Encoder[CNil] = new Encoder[CNil] {
    override def apply(value: CNil): Reader[SchemaData, Result[Avro]] =
      Error("encoding CNil should never happen").asLeft[Avro].liftR
  }

  implicit def coproductEncoder[H, T <: Coproduct](implicit hEncoder: Encoder[H],
                                                   tEncoder: Encoder[T]): Encoder[H :+: T] = new Encoder[H :+: T] {
    override def apply(value: H :+: T): Reader[SchemaData, Result[Avro]] =
      value match {
        case Inl(h) => hEncoder(h)
        case Inr(v) => tEncoder(v)
      }
  }

}
