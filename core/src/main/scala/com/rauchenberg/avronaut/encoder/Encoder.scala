package com.rauchenberg.avronaut.encoder

import java.time.{Instant, OffsetDateTime}
import java.util
import java.util.UUID

import com.rauchenberg.avronaut.common.Error
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.{GenericData, GenericRecord}
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}
import cats.syntax.either._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Encoder[A] {

  type Ret

  def isRecord: Boolean = false

  def apply(value: A, schemaData: SchemaData, failFast: Boolean): Ret

}

object Encoder {

  def apply[A](implicit encoder: Encoder[A]) = encoder

  type Typeclass[A] = Encoder[A]

  implicit def gen[A]: Encoder[A] = macro Magnolia.gen[A]

  def encode[A](a: A,
                encoder: Encoder[A],
                schemaData: Either[List[Error], SchemaData]): Either[List[Error], GenericRecord] =
    runEncoder(a, encoder, schemaData, true)

  def encodeAccumulating[A](a: A,
                            encoder: Encoder[A],
                            schemaData: Either[List[Error], SchemaData]): Either[List[Error], GenericRecord] =
    runEncoder(a, encoder, schemaData, false)

  private def runEncoder[A](a: A,
                            encoder: Encoder[A],
                            schemaData: Either[List[Error], SchemaData],
                            failFast: Boolean): Either[List[Error], GenericRecord] =
    schemaData.flatMap { schema =>
      encoder.apply(a, schema, failFast) match {
        case Right(gr: GenericRecord) => Right(gr)
        case Left(l: List[_])         => l.asInstanceOf[List[Error]].asLeft[GenericRecord]
        case _                        => List(Error("shit")).asLeft[GenericRecord]
      }
    }

  def combine[A](ctx: CaseClass[Typeclass, A]): Encoder[A] =
    new Encoder[A] {

      override def isRecord = true

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      type Ret = Either[List[Error], GenericRecord]
      val DOT        = "."
      val recordName = namespace.concat(DOT).concat(name)

      def errorStr[C](param: String, value: C): String =
        "Encoding failed for param '".concat(param).concat("' with value '").concat(value + "'")

      def errorStr[C, D](param: String, value: C, originalMsg: D): String =
        errorStr(param, value).concat(", original message '").concat(originalMsg + "'")

      override def apply(value: A, sd: SchemaData, failFast: Boolean): Either[List[Error], GenericRecord] =
        sd.schemaMap.get(recordName) match {
          case None => Left(List(Error(s"couldn't find a schema field for " + recordName)))
          case Some(schema) =>
            val gr        = new GenericData.Record(schema)
            var cnt       = 0
            val fields    = schema.getFields.asScala.map(_.name)
            var hasErrors = false
            val errors    = ListBuffer[Error]()
            if (!failFast) {
              ctx.parameters.foreach { param =>
                if (fields.contains(param.label)) {
                  val paramValue = param.dereference(value)
                  try {
                    param.typeclass.apply(param.dereference(value), sd, failFast) match {
                      case Right(v) => gr.put(cnt, v)
                      case Left(Error(msg)) =>
                        errors += Error(errorStr(param.label, paramValue, msg))
                      case Error(_) => ()
                      case other    => gr.put(cnt, other)
                    }
                  } catch {
                    case scala.util.control.NonFatal(_) =>
                      println("here!")
                      errors += Error(errorStr(param.label, paramValue))
                      hasErrors = true
                  }
                  cnt = cnt + 1
                }
              }
            } else {
              val it = ctx.parameters.iterator
              while (it.hasNext && !hasErrors) {
                val param = it.next
                if (fields.contains(param.label)) {
                  val paramValue = param.dereference(value)
                  try {
                    param.typeclass.apply(param.dereference(value), sd, failFast) match {
                      case Right(v) => gr.put(cnt, v)
                      case Left(Error(msg)) =>
                        errors += Error(errorStr(param.label, paramValue, msg))
                        hasErrors = true
                      case Error(_) => ()
                      case other    => gr.put(cnt, other)
                    }
                    cnt = cnt + 1
                  } catch {
                    case scala.util.control.NonFatal(_) =>
                      errors += Error(errorStr(param.label, paramValue))
                      hasErrors = true
                  }

                }
              }
            }
            if (hasErrors) {
              Left(errors.toList)
            } else Right(gr)

        }
    }

  def dispatch[A : WeakTypeTag](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new Encoder[A] {
    override type Ret = Any
    import com.rauchenberg.avronaut.common.ReflectionHelpers._

    override def apply(value: A, schemaData: SchemaData, failFast: Boolean): Ret =
      ctx.dispatch(value) { subtype =>
        if (isEnum) value.toString
        else {
          value match {
            case p: Product if p.productArity == 0 => p.toString
            case _                                 => subtype.typeclass(value.asInstanceOf[subtype.SType], schemaData, failFast)
          }
        }
      }
  }

  implicit val stringEncoder: Encoder[String] = new Encoder[String] {
    type Ret = String
    override def apply(value: String, schemaData: SchemaData, failFast: Boolean): String = value
  }

  implicit val boolEncoder: Encoder[Boolean] = new Encoder[Boolean] {
    type Ret = Boolean
    override def apply(value: Boolean, schemaData: SchemaData, failFast: Boolean): Boolean = value
  }

  implicit val intEncoder: Encoder[Int] = new Encoder[Int] {
    type Ret = Int
    override def apply(value: Int, schemaData: SchemaData, failFast: Boolean): Int = java.lang.Integer.valueOf(value)
  }

  implicit val floatEncoder: Encoder[Float] = new Encoder[Float] {
    type Ret = Float
    override def apply(value: Float, schemaData: SchemaData, failFast: Boolean): Float = value
  }

  implicit val doubleEncoder: Encoder[Double] = new Encoder[Double] {
    type Ret = Double
    override def apply(value: Double, schemaData: SchemaData, failFast: Boolean): Double = value
  }

  implicit val longEncoder: Encoder[Long] = new Encoder[Long] {
    type Ret = Long
    override def apply(value: Long, schemaData: SchemaData, failFast: Boolean): Long = value
  }

  implicit val bytesEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
    override type Ret = Array[Byte]

    override def apply(value: Array[Byte], schemaData: SchemaData, failFast: Boolean): Array[Byte] = value
  }

  implicit def mapEncoder[A](implicit aEncoder: Encoder[A]): Encoder[Map[String, A]] =
    new Encoder[Map[String, A]] {
      override type Ret = java.util.Map[String, Any]
      override def apply(value: Map[String, A], schemaData: SchemaData, failFast: Boolean): Ret = {
        val it = value.iterator
        val hm = new java.util.HashMap[String, Any](value.size)
        if (aEncoder.isRecord) {
          while (it.hasNext) {
            val (k, v) = it.next
            hm.put(k, (aEncoder(v, schemaData, failFast) match { case Right(v) => v }))
          }
          hm
        } else {
          while (it.hasNext) {
            val (k, v) = it.next
            hm.put(k, (aEncoder(v, schemaData, failFast)))
          }
          hm
        }
      }

    }

  implicit def listEncoder[A : ClassTag](implicit aEncoder: Encoder[A]): Encoder[List[A]] = new Encoder[List[A]] {

    type Ret = java.util.List[Any]

    override def apply(value: List[A], schemaData: SchemaData, failFast: Boolean): Ret = {
      val arr = new util.ArrayList[Any](value.size)
      val it  = value.iterator
      if (aEncoder.isRecord) {
        while (it.hasNext) {
          arr.add(aEncoder(it.next, schemaData, failFast) match {
            case Right(v) => v
          })
        }
      } else {
        while (it.hasNext) {
          arr.add(aEncoder(it.next, schemaData, failFast))
        }
      }
      arr
    }

  }

  implicit def optionEncoder[A](implicit aEncoder: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {

    type Ret = Any

    override def apply(value: Option[A], schemaData: SchemaData, failFast: Boolean): Ret =
      value.fold[Any](null)(v => aEncoder(v, schemaData, failFast))
  }

  implicit def eitherEncoder[A, B](implicit aEncoder: Encoder[A], bEncoder: Encoder[B]) =
    new Encoder[Either[A, B]] {
      override type Ret = Any

      override def apply(value: Either[A, B], schemaData: SchemaData, failFast: Boolean): Ret =
        value.fold(aEncoder(_, schemaData, failFast), bEncoder(_, schemaData, failFast))
    }

  implicit val cnilEncoder: Encoder[CNil] = new Encoder[CNil] {
    override type Ret = Error

    override def apply(value: CNil, schemaData: SchemaData, failFast: Boolean): Error = Error("should never get a CNil")
  }

  implicit def coproductEncoder[A, B <: Coproduct](implicit aEncoder: Encoder[A],
                                                   bEncoder: Encoder[B]): Encoder[A :+: B] = new Encoder[A :+: B] {
    override type Ret = Any

    override def apply(value: A :+: B, schemaData: SchemaData, failFast: Boolean): Ret =
      value match {
        case Inl(a) => aEncoder(a, schemaData, failFast)
        case Inr(b) => bEncoder(b, schemaData, failFast)
      }
  }

  implicit val uuidEncoder: Encoder[UUID] = new Encoder[UUID] {
    override type Ret = String
    override def apply(value: UUID, schemaData: SchemaData, failFast: Boolean): String = value.toString
  }

  implicit val instantEncoder: Encoder[Instant] = new Encoder[Instant] {
    override type Ret = Long
    override def apply(value: Instant, schemaData: SchemaData, failFast: Boolean): Long = value.toEpochMilli
  }

  implicit val dateTimeEncoder: Encoder[OffsetDateTime] = new Encoder[OffsetDateTime] {
    override type Ret = Long
    override def apply(value: OffsetDateTime, schemaData: SchemaData, failFast: Boolean): Long =
      value.toInstant.toEpochMilli
  }

}
