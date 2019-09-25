package com.rauchenberg.avronaut.common

import cats.syntax.either._

import scala.collection.mutable.ListBuffer

sealed abstract class AvroType extends Product with Serializable { self =>

  def findAllByKey[A](key: String): List[AvroType] = {
    val hh: ListBuffer[AvroType] = ListBuffer.empty[AvroType]
    def loop(avroType: AvroType): Unit = avroType match {
      case a @ AvroRecord(avfn, values) =>
        if (avfn == key) hh += a
        values.foreach(loop(_))
      case a @ AvroField(fieldName, _)           => if (fieldName == key) hh += a
      case a @ AvroUnion(fieldName, _)           => if (fieldName == key) hh += a
      case a @ AvroArray(fieldName, _)           => if (fieldName == key) hh += a
      case a @ AvroEnum(fieldName, _)            => if (fieldName == key) hh += a
      case a @ AvroUUID(fieldName, _)            => if (fieldName == key) hh += a
      case a @ AvroTimestampMillis(fieldName, _) => if (fieldName == key) hh += a
      case a @ AvroMap(fieldName, _)             => if (fieldName == key) hh += a
      case a @ ParseFail(fieldName, _)           => if (fieldName == key) hh += a
      case _                                     => // do nothing
    }
    loop(this)
    hh.toList
  }

  def isNull = this match {
    case AvroNull => true
    case _        => false
  }

}
final case object AvroNull                                               extends AvroType
final case class AvroInt(value: Int)                                     extends AvroType
final case class AvroLong(value: Long)                                   extends AvroType
final case class AvroFloat(value: Float)                                 extends AvroType
final case class AvroDouble(value: Double)                               extends AvroType
final case class AvroBoolean(value: Boolean)                             extends AvroType
final case class AvroString(value: String)                               extends AvroType
final case class AvroRecord(fieldName: String, value: List[AvroType])    extends AvroType
final case class AvroEnum[A](fieldName: String, value: A)                extends AvroType
final case class AvroUnion(fieldName: String, value: AvroType)           extends AvroType
final case class AvroArray(fieldName: String, value: List[AvroType])     extends AvroType
final case class AvroMapEntry(key: String, value: AvroType)              extends AvroType
final case class AvroMap(fieldName: String, value: List[AvroMapEntry])   extends AvroType
final case class AvroBytes(value: Array[Byte])                           extends AvroType
final case class AvroField(fieldName: String, value: AvroType)           extends AvroType
final case class AvroUUID(fieldName: String, value: AvroType)            extends AvroType
final case class ParseFail(fieldName: String, msg: String)               extends AvroType
final case class AvroTimestampMillis(fieldName: String, value: AvroType) extends AvroType

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

  final def toAvroRecord(fieldName: String, value: List[AvroType])   = safe(AvroRecord(fieldName, value))
  final def toAvroRecord(fieldName: String, value: Vector[AvroType]) = safe(AvroRecord(fieldName, value.toList))
  final def toAvroArray(fieldName: String, value: List[AvroType])    = safe(AvroArray(fieldName, value))
  final def toAvroArray(fieldName: String, value: Vector[AvroType])  = safe(AvroArray(fieldName, value.toList))
  final def toAvroUnion(fieldName: String, value: AvroType)          = safe(AvroUnion(fieldName, value))
  final def toAvroEnum[A](fieldName: String, value: A)               = safe(AvroEnum(fieldName, value))

  final def toAvroUUID[A](fieldName: String, value: A) = toAvroString(value).map(AvroUUID(fieldName, _))

  final def toAvroTimestamp[A](fieldName: String, value: A) = toAvroLong(value).map(AvroTimestampMillis(fieldName, _))
}
