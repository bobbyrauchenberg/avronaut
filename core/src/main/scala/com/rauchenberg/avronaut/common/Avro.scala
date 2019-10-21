package com.rauchenberg.avronaut.common

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import cats.syntax.either._
import org.apache.avro.Schema

sealed trait Avro {
  type Ret
  val value: Ret
}
final case object AvroNull extends Avro {
  type Ret = Null
  override val value: Ret = null
}
final case class AvroInt(value: Int) extends Avro {
  type Ret = Int
}
final case class AvroFloat(value: Float) extends Avro {
  type Ret = Float
}
final case class AvroDouble(value: Double) extends Avro {
  type Ret = Double
}
final case class AvroLong(value: Long) extends Avro {
  type Ret = Long
}
final case class AvroBoolean(value: Boolean) extends Avro {
  type Ret = Boolean
}
final case class AvroString(value: String) extends Avro {
  type Ret = String
}
final case class AvroEnum[A](value: A) extends Avro {
  type Ret = A
}
final case class AvroUnion(value: Avro) extends Avro {
  type Ret = Avro
}
final case class AvroArray(value: List[Avro]) extends Avro {
  type Ret = List[Avro]
}
final case class AvroMap(value: List[(String, Avro)]) extends Avro {
  type Ret = List[(String, Avro)]
}
final case class AvroBytes(value: Array[Byte]) extends Avro {
  type Ret = Array[Byte]
}
final case class AvroLogical(value: Avro) extends Avro {
  type Ret = Avro
}
final case class AvroRecord(schema: Schema, value: List[Avro]) extends Avro {
  type Ret = List[Avro]
}
final case class AvroRoot(schema: Schema, value: List[Avro]) extends Avro {
  type Ret = List[Avro]
}
final case object AvroDecode extends Avro {
  type Ret = String
  override val value = ""
}

object Avro {

  final def toAvroString[A](value: A): Result[Avro] = value match {
    case v: java.lang.String => safe(AvroString(v))
    case _                   => Error(s"'$value' is not a String").asLeft
  }

  final def fromAvroString(value: Avro): Result[String] = value match {
    case AvroString(s) => s.asRight
    case _             => Error(s"$value is not an AvroString").asLeft
  }

  final def toAvroInt[A](value: A): Result[Avro] = value match {
    case v: java.lang.Integer => AvroInt(v).asRight
    case _                    => Error(s"'$value' is not an Int").asLeft
  }

  final def toAvroFloat[A](value: A): Result[Avro] = value match {
    case v: java.lang.Float => AvroFloat(v).asRight
    case _                  => Error(s"'$value' is not an Int").asLeft
  }

  final def toAvroDouble[A](value: A): Result[Avro] = value match {
    case v: java.lang.Double => AvroDouble(v).asRight
    case _                   => Error(s"'$value' is not an Int").asLeft
  }

  final def toAvroLong[A](value: A): Result[Avro] = value match {
    case v: java.lang.Long => AvroLong(v).asRight
    case _                 => Error(s"'$value' is not a Long").asLeft
  }

  final def fromAvroLong(value: Avro): Result[Long] = value match {
    case AvroLong(v) => v.asRight
    case _           => Error(s"$value is not an AvroLong").asLeft
  }

  final def fromAvroFloat(value: Avro): Result[Float] = value match {
    case AvroFloat(v) => v.asRight
    case _            => Error(s"$value is not an AvroFloat").asLeft
  }

  final def fromAvroDouble(value: Avro): Result[Double] = value match {
    case AvroDouble(v) => v.asRight
    case _             => Error(s"$value is not an AvroDouble").asLeft
  }

  final def toAvroBoolean[A](value: A): Result[Avro] = value match {
    case v: java.lang.Boolean => safe(AvroBoolean(v))
    case _                    => Error(s"'$value' is not a Boolean").asLeft
  }

  final def fromAvroBoolean(value: Avro): Result[Boolean] = value match {
    case AvroBoolean(s) => s.asRight
    case _              => Error(s"$value is not an AvroBoolean").asLeft
  }

  final def toAvroBytes[A](value: A): Result[Avro] = value match {
    case v: Array[Byte] => AvroBytes(v).asRight
    case _              => Error(s"'$value' is not an Array[Byte]").asLeft
  }

  final def fromAvroBytes(value: Avro): Result[Array[Byte]] = value match {
    case AvroBytes(s) => s.asRight
    case _            => Error(s"$value is not an AvroBytes").asLeft
  }

  final def toAvroNull[A](value: A) =
    if (value == null) AvroNull.asRight
    else Error(s"$value is not null").asLeft

  final def fromAvroNull(value: Avro): Result[None.type] = value match {
    case AvroNull => Right(null)
    case _        => Error(s"$value is not an AvroNull").asLeft
  }

  final def toAvroRecord(value: List[Avro])   = safe(AvroRecord(null, value))
  final def toAvroRecord(value: Vector[Avro]) = safe(AvroRecord(null, value.toList))
  final def toAvroArray(value: List[Avro])    = safe(AvroArray(value))
  final def toAvroArray(value: Vector[Avro])  = safe(AvroArray(value.toList))
  final def toAvroUnion(value: Avro)          = safe(AvroUnion(value))
  final def toAvroEnum[A](value: A)           = safe(AvroEnum(value))

  final def toAvroUUID[A](value: A)        = toAvroString(value).map(AvroLogical(_))
  final def uuidToAvroLogical(value: UUID) = toAvroString(value.toString).map(AvroLogical(_))

  final def dateTimeToAvroLogical(value: OffsetDateTime) =
    safe(value.toInstant.toEpochMilli).flatMap(toAvroLong).map(AvroLogical(_))

  final def instantToAvroLogical(value: Instant) = safe(value.toEpochMilli).flatMap(toAvroLong).map(AvroLogical(_))

  final def toAvroTimestamp[A](value: A) = toAvroLong(value).map(AvroLogical(_))
}
