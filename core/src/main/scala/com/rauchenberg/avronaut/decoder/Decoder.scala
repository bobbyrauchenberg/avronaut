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
import scala.reflect.ClassTag

trait Decoder[A] {

  def apply[B](value: B, genericRecord: GenericRecord): Result[A]

}

object Decoder {

  type Typeclass[A] = Decoder[A]

  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]

  def apply[A](implicit decoder: Decoder[A]) = decoder

  def decode[A](genericRecord: GenericRecord, decoder: Decoder[A]): Result[A] =
    decoder("", genericRecord)

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    val params = ctx.parameters.toArray

    override def apply[B](value: B, genericRecord: GenericRecord): Result[A] = {

      val it     = params.iterator
      var failed = false
      var cnt    = 0
      val arr    = new Array[Any](params.size)

      while (it.hasNext && !failed) {
        val param            = it.next()
        val paramAnnotations = getAnnotations(param.annotations)
        val paramName        = paramAnnotations.name(param.label)

        val res: Result[param.PType] = valueOrDefault(
          try {
            genericRecord.get(paramName) match {
              case gr: GenericRecord =>
                param.typeclass.apply(gr, gr)
              case _ =>
                param.typeclass.apply(genericRecord.get(paramName), genericRecord)
            }
          } catch {
            case scala.util.control.NonFatal(_) =>
              Left(Error("failed decoding param : " + paramName))
          },
          param.default
        )
        if (res.isLeft) failed = true
        else {
          arr(cnt) = res.right.get
          cnt = cnt + 1
        }
      }
      if (!failed) Right(ctx.rawConstruct(arr))
      else Left(Error("failed"))

    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Typeclass[A] {

    override def apply[B](value: B, genericRecord: GenericRecord): Result[A] =
      ctx.subtypes
        .find(_.typeName.short == value.toString)
        .map(st => ReflectionHelpers.toCaseObject[A](st.typeName.full))
        .fold[Result[A]](Left(Error("no")))(Right(_))
  }

  private def valueOrDefault[B](value: Result[B], default: Option[B]): Result[B] =
    (value, default) match {
      case (Right(value), _)        => Right(value)
      case (Left(_), Some(default)) => Right(default)
      case _                        => Left(Error("blah"))
    }

  def error[A](expected: String, actual: A): Either[Error, Nothing] = Left(Error(s"expected $expected, got $actual"))

  implicit val stringDecoder: Decoder[String] = new Decoder[String] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[String] = value match {
      case s: String      => Right(s)
      case u: Utf8        => Right(u.toString)
      case a: Array[Byte] => Right(new String(a))
    }
  }

  implicit val booleanDecoder: Decoder[Boolean] = new Decoder[Boolean] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Boolean] =
      value match {
        case true  => Right(true)
        case false => Right(false)
      }
  }

  implicit val intDecoder: Decoder[Int] = new Decoder[Int] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Int] = Right(value.toString.toInt)
  }

  implicit val longDecoder: Decoder[Long] = new Decoder[Long] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Long] = value match {
      case l: Long => Right(l)
    }
  }

  implicit val floatDecoder: Decoder[Float] = new Decoder[Float] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Float] = value match {
      case f: Float => Right(f)
    }

  }

  implicit val doubleDecoder: Decoder[Double] = new Decoder[Double] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Double] = value match {
      case d: Double => Right(d)
    }
  }

  implicit val bytesDecoder: Decoder[Array[Byte]] = new Decoder[Array[Byte]] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Array[Byte]] =
      Right(value.asInstanceOf[Array[Byte]])
  }

  implicit def listDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[List[A]] =
    new Typeclass[List[A]] {
      override def apply[B](value: B, genericRecord: GenericRecord): Result[List[A]] = {
        val list   = value.asInstanceOf[java.util.List[A]]
        val arr    = new Array[A](list.size)
        val it     = list.iterator()
        var cnt    = 0
        var failed = false
        while (it.hasNext && !failed) {
          val x = it.next()
          val el = x match {
            case gr: GenericRecord =>
              elementDecoder(gr, gr)
            case _ =>
              elementDecoder(x, genericRecord)
          }
          if (el.isRight) arr(cnt) = el.right.get
          else
            failed = true
          cnt = cnt + 1
        }
        if (failed == true) Left(Error("couldn't parse list"))
        else Right(arr.toList)
      }
    }

  implicit def seqDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[Seq[A]] = new Decoder[Seq[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Seq[A]] =
      listDecoder[A].apply[B](value, genericRecord)
  }

  implicit def vectorDecoder[A : ClassTag](implicit elementDecoder: Decoder[A]): Decoder[Vector[A]] =
    new Decoder[Vector[A]] {
      override def apply[B](value: B, genericRecord: GenericRecord): Result[Vector[A]] =
        listDecoder[A].apply[B](value, genericRecord).map(_.toVector)

    }

  implicit def mapDecoder[A](implicit elementDecoder: Decoder[A]): Decoder[Map[String, A]] =
    new Decoder[Map[String, A]] {
      override def apply[B](value: B, genericRecord: GenericRecord): Result[Map[String, A]] = {
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
              elementDecoder(gr, gr) match {
                case Left(_) =>
                  isFailed = true
                case Right(v) => arr(cnt) = (k -> v)
              }
            case _ =>
              elementDecoder(v, genericRecord) match {
                case Left(_)  => isFailed = true
                case Right(v) => arr(cnt) = (k -> v)
              }
          }
          cnt = cnt + 1
        }
        if (isFailed) Left(Error("couldn't decode map"))
        else Right(arr.toMap)
      }

    }

  implicit def offsetDateTimeDecoder: Decoder[OffsetDateTime] = new Decoder[OffsetDateTime] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[OffsetDateTime] =
      value match {
        case l: Long => Right(OffsetDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC))
      }

  }

  implicit def instantDecoder: Decoder[Instant] = new Decoder[Instant] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Instant] = value match {
      case l: Long => Right(Instant.ofEpochMilli(l))
    }
  }

  implicit def uuidDecoder: Decoder[UUID] = new Decoder[UUID] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[UUID] = value match {
      case s: String if (s != null) => Right(java.util.UUID.fromString(s))
    }
  }

  implicit def optionDecoder[A](implicit valueDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[Option[A]] =
      if (value == null) Right(None)
      else
        valueDecoder(value, genericRecord).map(Option(_))
  }

  implicit def eitherDecoder[A, B](implicit lDecoder: Decoder[A], rDecoder: Decoder[B]): Decoder[Either[A, B]] =
    new Decoder[Either[A, B]] {
      override def apply[C](value: C, genericRecord: GenericRecord): Result[Either[A, B]] =
        safe(rDecoder(value, genericRecord)).flatten match {
          case Right(v) => Right(Right(v))
          case Left(_) =>
            lDecoder(value, genericRecord) match {
              case Right(v) => Right(Left(v))
              case _        => Left(Error("couldn't decode either"))
            }
        }
    }

  implicit object CNilDecoderValue extends Decoder[CNil] {
    override def apply[B](value: B, genericRecord: GenericRecord): Result[CNil] =
      Error("Should not have got to CNil").asLeft
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H],
                                                   tDecoder: Decoder[T]): Decoder[H :+: T] = new Decoder[H :+: T] {
    type Ret = H :+: T
    override def apply[B](value: B, genericRecord: GenericRecord): Result[H :+: T] =
      safe(hDecoder(value, genericRecord)).flatten match {
        case Left(_)  => tDecoder(value, genericRecord).map(Inr(_))
        case Right(r) => Right(Coproduct[H :+: T](r))
      }
  }

}
