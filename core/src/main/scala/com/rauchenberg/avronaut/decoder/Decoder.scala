package com.rauchenberg.avronaut.decoder

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import cats.implicits._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.getAnnotations
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import com.rauchenberg.avronaut.common.ReflectionHelpers._
import scala.reflect.runtime.universe._

trait Decoder[A] {

  def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[A]

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def apply[A](implicit decoder: Decoder[A]) = decoder

  def decode[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    decoder("", genericRecord, true)

  def decodeAccumlating[A](genericRecord: GenericRecord, decoder: Decoder[A]): Results[A] =
    decoder("", genericRecord, false)

  def errorStr[C](param: String, value: C): String =
    "Decoding failed for param '".concat(param).concat("' with value '").concat(value + "' from the GenericRecord")

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val params = ctx.parameters.toArray

    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[A] = {

      val it     = params.iterator
      var failed = false
      var cnt    = 0
      val arr    = new Array[Any](params.size)

      val errors = new ListBuffer[Error]()

      def iterateFailFast = {
        while (it.hasNext && !failed) {
          val param            = it.next()
          val paramAnnotations = getAnnotations(param.annotations)
          val paramName        = paramAnnotations.name(param.label)
          val res = valueOrDefault(
            try {
              genericRecord.get(paramName) match {
                case gr: GenericRecord =>
                  param.typeclass.apply(gr, gr, failFast)
                case _ =>
                  param.typeclass.apply(genericRecord.get(paramName), genericRecord, failFast)
              }
            } catch {
              case scala.util.control.NonFatal(_) => Left(Nil)
            },
            param.default,
            param.label,
            value
          )
          res match {
            case Right(v) =>
              arr(cnt) = v
              cnt = cnt + 1
            case Left(l) =>
              errors.appendAll(l)
              failed = true
          }
        }
        if (!failed) Right(ctx.rawConstruct(arr))
        else Left(errors.toList :+ Error("The failing GenericRecord was: " + genericRecord.toString))
      }

      def iterateAccumulating = {
        while (it.hasNext) {
          val param            = it.next()
          val paramAnnotations = getAnnotations(param.annotations)
          val paramName        = paramAnnotations.name(param.label)
          val res = valueOrDefault(
            try {
              genericRecord.get(paramName) match {
                case gr: GenericRecord =>
                  param.typeclass.apply(gr, gr, failFast)
                case _ =>
                  param.typeclass.apply(genericRecord.get(paramName), genericRecord, failFast)
              }
            } catch {
              case scala.util.control.NonFatal(_) =>
                failed = true
                Left(Nil) // needed to compile, but errors are accumulated in `errors`
            },
            param.default,
            param.label,
            value
          )
          res match {
            case Right(v) =>
              arr(cnt) = v
            case Left(l) =>
              errors.appendAll(l)
              failed = true
          }
          cnt = cnt + 1
        }
        if (!failed) Right(ctx.rawConstruct(arr))
        else Left(errors.toList :+ Error("The failing GenericRecord was: " + genericRecord.toString))
      }

      if (failFast) {
        iterateFailFast
      } else {
        iterateAccumulating
      }
    }
  }

  def dispatch[A : WeakTypeTag](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val emptyFail = Left(Nil)

    def deriveEnum[B](value: B) =
      ctx.subtypes
        .find(_.typeName.short == value.toString)
        .map(st => ReflectionHelpers.toCaseObject[A](st.typeName.full))
        .fold[Results[A]](emptyFail)(Right(_))

    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[A] =
      if (isEnum) deriveEnum(value)
      else {
        value match {
          case gr: GenericRecord =>
            ctx.subtypes
              .find(_.typeName.full == gr.getSchema.getFullName)
              .map(_.typeclass.apply(value, genericRecord, failFast))
              .getOrElse(emptyFail)
          case _ => deriveEnum(value)
        }
      }
  }

  private def valueOrDefault[A, B](value: Results[B], default: Option[B], paramName: String, origValue: A): Results[B] =
    (value, default) match {
      case (Right(value), _)        => Right(value)
      case (Left(_), Some(default)) => Right(default)
      case _                        => Left(List(Error(errorStr(paramName, origValue))))
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Left(Error(s"expected $expected, got $actual"))

  implicit val stringDecoder: Decoder[String] = new Decoder[String] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[String] = value match {
      case s: String      => Right(s)
      case u: Utf8        => Right(u.toString)
      case a: Array[Byte] => Right(new String(a))
    }
  }

  implicit val booleanDecoder: Decoder[Boolean] = new Decoder[Boolean] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Boolean] =
      value match {
        case true  => Right(true)
        case false => Right(false)
      }
  }

  implicit val intDecoder: Decoder[Int] = new Decoder[Int] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Int] =
      Right(value.toString.toInt)
  }

  implicit val longDecoder: Decoder[Long] = new Decoder[Long] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Long] = value match {
      case l: Long => Right(l.toString.toLong)
    }
  }

  implicit val floatDecoder: Decoder[Float] = new Decoder[Float] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Float] = value match {
      case f: Float => Right(f.toString.toFloat)
    }

  }

  implicit val doubleDecoder: Decoder[Double] = new Decoder[Double] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Double] = value match {
      case d: Double => Right(d.toString.toDouble)
    }
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = new Decoder[Array[Byte]] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Array[Byte]] =
      Right(value.asInstanceOf[Array[Byte]])
  }

  implicit def listDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[List[A]] =
    new Typeclass[List[A]] {
      override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[List[A]] = {
        val list   = value.asInstanceOf[java.util.List[A]]
        val arr    = new Array[A](list.size)
        val it     = list.iterator()
        var cnt    = 0
        var failed = false
        while (it.hasNext && !failed) {
          val x = it.next()
          val el = x match {
            case gr: GenericRecord =>
              elementDecoder(gr, gr, failFast)
            case _ =>
              elementDecoder(x, genericRecord, failFast)
          }
          if (el.isRight) {
            arr(cnt) = el.right.get
            cnt = cnt + 1
          } else
            failed = true

        }
        if (failed) Left(List(Error(s"failed list decode on element '$cnt'")))
        else Right(arr.toList)
      }
    }

  implicit def seqDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[Seq[A]] = new Decoder[Seq[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Seq[A]] =
      listDecoder[A].apply[B](value, genericRecord, failFast)
  }

  implicit def vectorDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[Vector[A]] =
    new Decoder[Vector[A]] {
      override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Vector[A]] =
        listDecoder[A].apply[B](value, genericRecord, failFast).map(_.toVector)

    }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] =
    new Decoder[Map[String, A]] {
      override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Map[String, A]] = {
        var cnt      = 0
        var isFailed = false

        val map = value
          .asInstanceOf[java.util.Map[String, A]]

        val it = map.asScala.toArray.iterator

        val arr = new Array[(String, A)](map.size)

        while (it.hasNext && !isFailed) {
          val (k, v) = it.next
          v match {
            case gr: GenericRecord =>
              elementDecoder(gr, gr, failFast) match {
                case Left(_) =>
                  isFailed = true
                case Right(v) => arr(cnt) = (k -> v)
              }
            case _ =>
              elementDecoder(v, genericRecord, failFast) match {
                case Left(_)  => isFailed = true
                case Right(v) => arr(cnt) = (k -> v)
              }
          }
          cnt = cnt + 1
        }
        if (isFailed) Left(List(Error("couldn't decode map")))
        else Right(arr.toMap)
      }

    }

  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = new Decoder[OffsetDateTime] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[OffsetDateTime] =
      value match {
        case l: Long => Right(OffsetDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC))
      }

  }

  implicit def instantDecoder: Decoder[Instant] = new Decoder[Instant] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Instant] = value match {
      case l: Long => Right(Instant.ofEpochMilli(l))
    }
  }

  implicit def uuidDecoder: Decoder[UUID] = new Decoder[UUID] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[UUID] = value match {
      case s: String if (s != null) => Right(java.util.UUID.fromString(s))
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[Option[A]] =
      if (value == null) Right(None)
      else
        valueDecoder(value, genericRecord, failFast).map(Option(_))
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
    new Decoder[Either[A, B]] {
      override def apply[C](value: C, genericRecord: GenericRecord, failFast: Boolean): Results[Either[A, B]] =
        safeL(rDecoder(value, genericRecord, failFast)).flatten match {
          case Right(v) => Right(Right(v))
          case Left(_) =>
            lDecoder(value, genericRecord, failFast) match {
              case Right(v) => Right(Left(v))
              case _        => Left(List(Error("couldn't decode either")))
            }
        }
    }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[CNil] =
      List(Error("Should not have got to CNil")).asLeft
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = new Decoder[H :+: T] {
    type Ret = H :+: T
    override def apply[B](value: B, genericRecord: GenericRecord, failFast: Boolean): Results[H :+: T] =
      safeL(hDecoder(value, genericRecord, failFast)).flatten match {
        case Left(_)  => tDecoder(value, genericRecord, failFast).map(Inr(_))
        case Right(r) => Right(Coproduct[H :+: T](r))
      }
  }

}
