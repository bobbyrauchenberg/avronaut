package com.rauchenberg.avronaut.common

import cats.syntax.either._

sealed trait AvroType
final case object AvroNull                                  extends AvroType
final case class AvroNumber(value: AvroNum)                 extends AvroType
final case class AvroBoolean(value: Boolean)                extends AvroType
final case class AvroString(value: String)                  extends AvroType
final case class AvroRecord(value: List[AvroType])          extends AvroType
final case class AvroEnum[A](value: A)                      extends AvroType
final case class AvroUnion(value: AvroType)                 extends AvroType
final case class AvroArray(value: List[AvroType])           extends AvroType
final case class AvroMapEntry(key: String, value: AvroType) extends AvroType
final case class AvroMap(value: List[AvroMapEntry])         extends AvroType
final case class AvroBytes(value: Array[Byte])              extends AvroType
final case class AvroLogicalType(value: AvroType)           extends AvroType

object AvroType {

  final def toAvroString[A](value: A): Result[AvroType] = value match {
    case v: java.lang.String => safe(AvroString(v))
    case _                   => Error(s"'$value' is not a String").asLeft
  }

  final def fromAvroString(value: AvroType): Result[String] = value match {
    case AvroString(s) => s.asRight
    case _             => Error(s"$value is not an AvroString").asLeft
  }

  final def toAvroInt[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Integer => safe(AvroNumber(AvroNumLong(v.longValue)))
    case _                    => Error(s"'$value' is not an Int").asLeft
  }

  final def fromAvroInt(value: AvroType): Result[Int] =
    value match {
      case AvroNumber(v) => v.toInt
      case _             => Error(s"$value is not an AvroInt").asLeft
    }

  final def toAvroLong[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Long => safe(AvroNumber(AvroNumLong(v)))
    case _                 => Error(s"'$value' is not a Long").asLeft
  }

  final def fromAvroLong(value: AvroType): Result[Long] = value match {
    case AvroNumber(v) => v.toLong
    case _             => Error(s"$value is not an AvroLong").asLeft
  }

  final def toAvroFloat[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Float => safe(AvroNumber(AvroNumFloat(v)))
    case _                  => Error(s"'$value' is not a Float").asLeft
  }

  final def fromAvroFloat(value: AvroType): Result[Float] = value match {
    case AvroNumber(s) => s.toFloat.asRight
    case _             => Error(s"$value is not an AvroFloat").asLeft
  }

  final def toAvroDouble[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Double => safe(AvroNumber(AvroNumDouble(v)))
    case _                   => Error(s"'$value' is not a Double").asLeft
  }

  final def fromAvroDouble(value: AvroType): Result[Double] = value match {
    case AvroNumber(s) => s.toDouble.asRight
    case _             => Error(s"$value is not an AvroDouble").asLeft
  }

  final def toAvroBoolean[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Boolean => safe(AvroBoolean(v))
    case _                    => Error(s"'$value' is not a Boolean").asLeft
  }

  final def fromAvroBoolean(value: AvroType): Result[Boolean] = value match {
    case AvroBoolean(s) => s.asRight
    case _              => Error(s"$value is not an AvroBoolean").asLeft
  }

  final def toAvroBytes[A](value: A) = value match {
    case v: Array[Byte] => safe(AvroBytes(v))
    case _              => Error(s"'$value' is not an Array[Byte]").asLeft
  }

  final def fromAvroBytes(value: AvroType): Result[Array[Byte]] = value match {
    case AvroBytes(s) => s.asRight
    case _            => Error(s"$value is not an AvroBytes").asLeft
  }

  final def toAvroNull[A](value: A) =
    if (value == null) AvroNull.asRight
    else Error(s"$value is not null").asLeft

  final def fromAvroNull(value: AvroType): Result[None.type] = value match {
    case AvroNull => Right(null)
    case _        => Error(s"$value is not an AvroNull").asLeft
  }

  final def toAvroRecord(value: List[AvroType])   = safe(AvroRecord(value))
  final def toAvroRecord(value: Vector[AvroType]) = safe(AvroRecord(value.toList))
  final def toAvroArray(value: List[AvroType])    = safe(AvroArray(value))
  final def toAvroArray(value: Vector[AvroType])  = safe(AvroArray(value.toList))
  final def toAvroUnion(value: AvroType)          = safe(AvroUnion(value))
  final def toAvroEnum[A](value: A)               = safe(AvroEnum(value))

  final def toAvroUUID[A](value: A) = toAvroString(value).map(AvroLogicalType(_))

  final def toAvroTimestamp[A](value: A) = toAvroLong(value).map(AvroLogicalType(_))
}
