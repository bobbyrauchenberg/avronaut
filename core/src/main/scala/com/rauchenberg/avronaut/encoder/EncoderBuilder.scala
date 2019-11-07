package com.rauchenberg.avronaut.encoder

import java.time.{Instant, OffsetDateTime}
import java.util
import java.util.UUID

import com.rauchenberg.avronaut.common.ReflectionHelpers._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
import com.rauchenberg.avronaut.common.{Error, Results}
import com.rauchenberg.avronaut.schema.SchemaData
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

trait EncoderBuilder[A] {

  type Ret

  def isRecord: Boolean = false

  def apply(value: A, schemaData: SchemaData, failFast: Boolean): Ret

}

trait RootEncoder[A] extends EncoderBuilder[A] {
  type Ret = Results[GenericRecord]
}

case class Encodable[A](encoder: EncoderBuilder[A], schemaData: SchemaData)

object EncoderBuilder {

  def apply[A](implicit encoder: EncoderBuilder[A]) = encoder

  type Typeclass[A] = EncoderBuilder[A]

  implicit def gen[A]: EncoderBuilder[A] = macro Magnolia.gen[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): EncoderBuilder[A] =
    new EncoderBuilder[A] {

      override def isRecord = true

      val annotations       = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)

      type Ret = Results[GenericRecord]

      val DOT        = "."
      val recordName = namespace.concat(DOT).concat(name)

      def errorStr[C](param: String, value: C): String =
        "Encoding failed for param '".concat(param).concat("' with value '").concat(value + "'")

      def errorStr[C, D](param: String, value: C, originalMsg: D): String =
        errorStr(param, value).concat(", original message '").concat(originalMsg + "'")

      override def apply(value: A, sd: SchemaData, failFast: Boolean): Results[GenericRecord] = {

        var cnt       = 0
        var hasErrors = false
        val errors    = ListBuffer[Error]()

        def iterateAccumulating(gr: GenericData.Record,
                                fields: collection.mutable.Buffer[String]): Results[GenericRecord] = {
          ctx.parameters.foreach { param =>
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
              } catch {
                case scala.util.control.NonFatal(_) =>
                  errors += Error(errorStr(param.label, paramValue))
                  hasErrors = true
              }
              cnt += 1
            }
          }
          if (hasErrors) {
            Left(errors.toList)
          } else Right(gr)
        }

        def iterateFailFast(gr: GenericData.Record,
                            fields: collection.mutable.Buffer[String]): Results[GenericRecord] = {
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
                cnt += 1
              } catch {
                case scala.util.control.NonFatal(_) =>
                  errors += Error(errorStr(param.label, paramValue))
                  hasErrors = true
              }
            }
          }
          if (hasErrors) {
            Left(errors.toList)
          } else Right(gr)
        }

        sd.schemaMap.get(recordName) match {
          case None => Left(List(Error(s"couldn't find a schema field for " + recordName)))
          case Some(schema) =>
            val gr     = new GenericData.Record(schema)
            val fields = schema.getFields.asScala.map(_.name)
            if (!failFast) {
              iterateAccumulating(gr, fields)
            } else {
              iterateFailFast(gr, fields)
            }
        }
      }
    }

  def dispatch[A : WeakTypeTag](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = new EncoderBuilder[A] {
    override type Ret = Any

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

  implicit val stringEncoder: EncoderBuilder[String] = new EncoderBuilder[String] {
    type Ret = Utf8
    override def apply(value: String, schemaData: SchemaData, failFast: Boolean): Utf8 = new Utf8(value)
  }

  implicit val boolEncoder: EncoderBuilder[Boolean] = new EncoderBuilder[Boolean] {
    type Ret = Boolean
    override def apply(value: Boolean, schemaData: SchemaData, failFast: Boolean): Boolean = value
  }

  implicit val intEncoder: EncoderBuilder[Int] = new EncoderBuilder[Int] {
    type Ret = Int
    override def apply(value: Int, schemaData: SchemaData, failFast: Boolean): Int = java.lang.Integer.valueOf(value)
  }

  implicit val floatEncoder: EncoderBuilder[Float] = new EncoderBuilder[Float] {
    type Ret = Float
    override def apply(value: Float, schemaData: SchemaData, failFast: Boolean): Float = value
  }

  implicit val doubleEncoder: EncoderBuilder[Double] = new EncoderBuilder[Double] {
    type Ret = Double
    override def apply(value: Double, schemaData: SchemaData, failFast: Boolean): Double = value
  }

  implicit val longEncoder: EncoderBuilder[Long] = new EncoderBuilder[Long] {
    type Ret = Long
    override def apply(value: Long, schemaData: SchemaData, failFast: Boolean): Long = value
  }

  implicit val bytesEncoder: EncoderBuilder[Array[Byte]] = new EncoderBuilder[Array[Byte]] {
    override type Ret = Array[Byte]

    override def apply(value: Array[Byte], schemaData: SchemaData, failFast: Boolean): Array[Byte] = value
  }

  implicit def mapEncoder[A](implicit aEncoder: EncoderBuilder[A]): EncoderBuilder[Map[String, A]] =
    new EncoderBuilder[Map[String, A]] {
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

  implicit def listEncoder[A](implicit aEncoder: EncoderBuilder[A]): EncoderBuilder[List[A]] =
    new EncoderBuilder[List[A]] {

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

  implicit def setEncoder[A](implicit aEncoder: EncoderBuilder[A]): EncoderBuilder[Set[A]] =
    new EncoderBuilder[Set[A]] {
      type Ret = EncoderBuilder[List[A]]#Ret

      override def apply(value: Set[A], schemaData: SchemaData, failFast: Boolean): EncoderBuilder[List[A]]#Ret =
        listEncoder[A].apply(value.toList, schemaData, failFast)
    }

  implicit def optionEncoder[A](implicit aEncoder: EncoderBuilder[A]): EncoderBuilder[Option[A]] =
    new EncoderBuilder[Option[A]] {

      type Ret = Any

      override def apply(value: Option[A], schemaData: SchemaData, failFast: Boolean): Ret =
        value.fold[Any](null)(v => aEncoder(v, schemaData, failFast))
    }

  implicit def eitherEncoder[A, B](implicit aEncoder: EncoderBuilder[A], bEncoder: EncoderBuilder[B]) =
    new EncoderBuilder[Either[A, B]] {
      override type Ret = Any

      override def apply(value: Either[A, B], schemaData: SchemaData, failFast: Boolean): Ret =
        value.fold(aEncoder(_, schemaData, failFast), bEncoder(_, schemaData, failFast))
    }

  implicit val cnilEncoder: EncoderBuilder[CNil] = new EncoderBuilder[CNil] {
    override type Ret = Error

    override def apply(value: CNil, schemaData: SchemaData, failFast: Boolean): Error = Error("should never get a CNil")
  }

  implicit def coproductEncoder[A, B <: Coproduct](implicit aEncoder: EncoderBuilder[A],
                                                   bEncoder: EncoderBuilder[B]): EncoderBuilder[A :+: B] =
    new EncoderBuilder[A :+: B] {
      override type Ret = Any

      override def apply(value: A :+: B, schemaData: SchemaData, failFast: Boolean): Ret =
        value match {
          case Inl(a) => aEncoder(a, schemaData, failFast)
          case Inr(b) => bEncoder(b, schemaData, failFast)
        }
    }

  implicit val uuidEncoder: EncoderBuilder[UUID] = new EncoderBuilder[UUID] {
    override type Ret = String
    override def apply(value: UUID, schemaData: SchemaData, failFast: Boolean): String = value.toString
  }

  implicit val instantEncoder: EncoderBuilder[Instant] = new EncoderBuilder[Instant] {
    override type Ret = Long
    override def apply(value: Instant, schemaData: SchemaData, failFast: Boolean): Long = value.toEpochMilli
  }

  implicit val dateTimeEncoder: EncoderBuilder[OffsetDateTime] = new EncoderBuilder[OffsetDateTime] {
    override type Ret = Long
    override def apply(value: OffsetDateTime, schemaData: SchemaData, failFast: Boolean): Long =
      value.toInstant.toEpochMilli
  }

}
