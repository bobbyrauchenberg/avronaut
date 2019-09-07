package com.rauchenberg.cupcatAvro.decoder.instances

import cats.implicits._
import com.rauchenberg.cupcatAvro.common._
import com.rauchenberg.cupcatAvro.decoder.Decoder
import org.apache.avro.generic.GenericRecord
import shapeless.{:+:, CNil, Coproduct, Inr}

import scala.reflect.runtime.universe._

object unionInstances extends unionInstances

trait unionInstances {

  implicit def optionDecoder[T](implicit someDecoder: Decoder[T]) = new Decoder[Option[T]] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[Option[T]] =
      if (record.get(fieldName) == null) none[T].asRight
      else someDecoder.decodeFrom(fieldName, record).map(Option.apply)
  }

  implicit def eitherDecoder[L, R](implicit leftDecoder: Decoder[L], ttL: TypeTag[L],
                                   rightDecoder: Decoder[R]) =
    new Decoder[Either[L, R]] {
      override def decodeFrom(fieldName: String, record: GenericRecord): Result[Either[L, R]] = {

        def decodeRight = rightDecoder.decodeFrom(fieldName, record)

        def decodeLeft = leftDecoder.decodeFrom(fieldName, record)

        val leftIsString = (ttL.tpe match {
          case TypeRef(_, us, _) => us
        }).fullName == "scala.Predef.String"

        if (leftIsString)
          decodeRight match {
            case Right(v) => v.asRight.asRight
            case _ => decodeLeft.map(_.asLeft)
          }
        else
          decodeLeft match {
            case Right(v) => v.asLeft.asRight
            case _ => decodeRight.map(_.asRight)
          }
      }

    }

  implicit def cnilDecoder = new Decoder[CNil] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[CNil] =
      Error(s"could not decode coproduct for fieldName: $fieldName").asLeft
  }

  implicit def coproductDecoder[H, T <: Coproduct](implicit hDecoder: Decoder[H], tDecoder: Decoder[T]) = {
    new Decoder[H :+: T] {
      override def decodeFrom(fieldName: String, record: GenericRecord): Result[H :+: T] = {
        hDecoder.decodeFrom(fieldName, record) match {
          case Right(v) =>
            v.toCP[H :+: T].asRight
          case _ =>
            tDecoder.decodeFrom(fieldName, record).map(Inr[H, T](_))
        }
      }
    }

  }


}

