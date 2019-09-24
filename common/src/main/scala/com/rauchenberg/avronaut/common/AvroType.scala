package com.rauchenberg.avronaut.common

import java.util.UUID

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
final case class AvroUUID(fieldName: String, value: UUID)                extends AvroType
final case class ParseFail(fieldName: String, msg: String)               extends AvroType
final case class AvroTimestampMillis(fieldName: String, value: AvroLong) extends AvroType

object AvroType {
  final val True  = AvroBoolean(true)
  final val False = AvroBoolean(false)

  final def fromBoolean(value: Boolean): AvroType   = if (value) True else False
  final def fromString(value: String): AvroType     = AvroString(value)
  final def fromInt(value: Int): AvroType           = AvroInt(value)
  final def fromLong(value: Long): AvroType         = AvroLong(value)
  final def fromFloat(value: Float): AvroType       = AvroFloat(value)
  final def fromDouble(value: Double): AvroType     = AvroDouble(value)
  final def fromBytes(value: Array[Byte]): AvroType = AvroBytes(value)

  final def toAvroString[A](value: A) =
    if (value.isInstanceOf[String]) safe(AvroString(value.toString))
    else Error(s"tried to create a string from '$value'").asLeft
  final def toAvroInt[A](value: A)    = safe(AvroInt(value.toString.toInt))
  final def toAvroLong[A](value: A)   = safe(AvroLong(value.toString.toLong))
  final def toAvroFloat[A](value: A)  = safe(AvroFloat(value.toString.toFloat))
  final def toAvroDouble[A](value: A) = safe(AvroDouble(value.toString.toDouble))
  final def toAvroBool[A](value: A)   = safe(AvroBoolean(value.toString.toBoolean))
  final def toAvroBytes[A](value: A)  = safe(AvroBytes(value.toString.getBytes))
  final def toAvroNull[A](value: A) =
    if (value == null || value == None) AvroNull.asRight
    else Error(s"$value is not null").asLeft
  final def toAvroRecord(fieldName: String, value: List[AvroType])   = safe(AvroRecord(fieldName, value))
  final def toAvroRecord(fieldName: String, value: Vector[AvroType]) = safe(AvroRecord(fieldName, value.toList))
  final def toAvroArray(fieldName: String, value: List[AvroType])    = safe(AvroArray(fieldName, value))
  final def toAvroArray(fieldName: String, value: Vector[AvroType])  = safe(AvroArray(fieldName, value.toList))
  final def toAvroUnion(fieldName: String, value: AvroType)          = safe(AvroUnion(fieldName, value))

  final def toAvroUUID[A](fieldName: String, value: A) =
    safe(java.util.UUID.fromString(value.toString)).map(AvroUUID(fieldName, _))

  final def toAvroTimestamp[A](fieldName: String, value: A) = toAvroLong(value).map(AvroTimestampMillis(fieldName, _))
}
