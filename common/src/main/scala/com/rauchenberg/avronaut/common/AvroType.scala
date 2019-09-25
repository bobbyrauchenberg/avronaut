package com.rauchenberg.avronaut.common

import cats.syntax.either._

sealed abstract class AvroType extends Product with Serializable { self =>

  def isNull = this match {
    case AvroNull => true
    case _        => false
  }

}
final case object AvroNull                                  extends AvroType
final case class AvroInt(value: Int)                        extends AvroType
final case class AvroLong(value: Long)                      extends AvroType
final case class AvroFloat(value: Float)                    extends AvroType
final case class AvroDouble(value: Double)                  extends AvroType
final case class AvroBoolean(value: Boolean)                extends AvroType
final case class AvroString(value: String)                  extends AvroType
final case class AvroRecord(value: List[AvroType])          extends AvroType
final case class AvroEnum[A](value: A)                      extends AvroType
final case class AvroUnion(value: AvroType)                 extends AvroType
final case class AvroArray(value: List[AvroType])           extends AvroType
final case class AvroMapEntry(key: String, value: AvroType) extends AvroType
final case class AvroMap(value: List[AvroMapEntry])         extends AvroType
final case class AvroBytes(value: Array[Byte])              extends AvroType
final case class AvroField(value: AvroType)                 extends AvroType
final case class AvroUUID(value: AvroType)                  extends AvroType
final case class AvroTimestampMillis(value: AvroType)       extends AvroType
final case class AvroFail(msg: String)                      extends AvroType

object AvroType {

  final def toAvroString[A](value: A): Result[AvroType] = value match {
    case v: java.lang.String => safe(AvroString(v))
    case _                   => Error(s"'$value' is not a String").asLeft
  }

  final def toAvroInt[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Integer => safe(AvroInt(v))
    case _                    => Error(s"'$value' is not an Int").asLeft
  }

  final def toAvroLong[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Long => safe(AvroLong(v))
    case _                 => Error(s"'$value' is not a Long").asLeft
  }

  final def toAvroFloat[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Float => safe(AvroFloat(v))
    case _                  => Error(s"'$value' is not a Float").asLeft
  }

  final def toAvroDouble[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Double => safe(AvroDouble(v))
    case _                   => Error(s"'$value' is not a Double").asLeft
  }

  final def toAvroBool[A](value: A): Result[AvroType] = value match {
    case v: java.lang.Boolean => safe(AvroBoolean(v))
    case _                    => Error(s"'$value' is not a Boolean").asLeft
  }

  final def toAvroBytes[A](value: A) = value match {
    case v: Array[Byte] => safe(AvroBytes(v))
    case _              => Error(s"'$value' is not Bytes").asLeft
  }

  final def toAvroNull[A](value: A) =
    if (value == null) AvroNull.asRight
    else Error(s"$value is not null").asLeft

  final def toAvroRecord(value: List[AvroType])   = safe(AvroRecord(value))
  final def toAvroRecord(value: Vector[AvroType]) = safe(AvroRecord(value.toList))
  final def toAvroArray(value: List[AvroType])    = safe(AvroArray(value))
  final def toAvroArray(value: Vector[AvroType])  = safe(AvroArray(value.toList))
  final def toAvroUnion(value: AvroType)          = safe(AvroUnion(value))
  final def toAvroEnum[A](value: A)               = safe(AvroEnum(value))

  final def toAvroUUID[A](value: A) = toAvroString(value).map(AvroUUID(_))

  final def toAvroTimestamp[A](value: A) = toAvroLong(value).map(AvroTimestampMillis(_))
}
