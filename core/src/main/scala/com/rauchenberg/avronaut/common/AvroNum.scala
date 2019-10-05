package com.rauchenberg.avronaut.common

import cats.syntax.either._

import java.math.{BigDecimal => JavaBigDecimal}

sealed abstract class AvroNum extends Serializable {

  def toBigDecimal: Result[BigDecimal]

  def toLong: Result[Long]

  def toBigInt: Result[BigInt]

  def toFloat: Float

  def toDouble: Double

  final def toInt: Result[Int] =
    toLong match {
      case Right(n) =>
        val asInt: Int = n.toInt
        if (n == asInt) Right(asInt) else Error(s"can't make Int from $n").asLeft
      case _ => Error(s"can't make Int from provided Long").asLeft
    }

}

object AvroNum {

  private[this] val bigDecimalMinLong: JavaBigDecimal = new JavaBigDecimal(Long.MinValue)
  private[this] val bigDecimalMaxLong: JavaBigDecimal = new JavaBigDecimal(Long.MaxValue)

  private[avronaut] def bigDecimalIsWhole(value: JavaBigDecimal): Boolean =
    value.signum == 0 || value.scale <= 0 || value.stripTrailingZeros.scale <= 0

  private[avronaut] def bigDecimalIsValidLong(value: JavaBigDecimal): Boolean =
    bigDecimalIsWhole(value) && value.compareTo(bigDecimalMinLong) >= 0 && value.compareTo(bigDecimalMaxLong) <= 0

}

private[avronaut] final case class AvroNumFloat(value: Float) extends AvroNum {
  import AvroNum._
  private[this] def toJavaBigDecimal = new JavaBigDecimal(java.lang.Float.toString(value))

  final def toBigDecimal: Result[BigDecimal] = (toJavaBigDecimal: BigDecimal).asRight
  final def toBigInt: Result[BigInt] = {
    val asBigDecimal = toJavaBigDecimal

    if (bigDecimalIsWhole(asBigDecimal)) new BigInt(asBigDecimal.toBigInteger).asRight
    else Error("Can't convert to BigInt").asLeft
  }

  final def toDouble: Double = toJavaBigDecimal.doubleValue

  final def toFloat: Float = value

  final def toLong: Result[Long] = {
    val asBigDecimal = toJavaBigDecimal
    if (bigDecimalIsValidLong(asBigDecimal)) asBigDecimal.longValue.asRight else Error("Can't convert to BigInt").asLeft
  }
}

final case class AvroNumLong(value: Long) extends AvroNum {
  final def toBigDecimal: Result[BigDecimal] = BigDecimal(value).asRight
  final def toBigInt: Result[BigInt]         = BigInt(value).asRight
  final def toDouble: Double                 = value.toDouble
  final def toFloat: Float                   = value.toFloat
  final def toLong: Result[Long]             = value.asRight
}

private[avronaut] final case class AvroNumDouble(value: Double) extends AvroNum {
  import AvroNum._
  private[this] def toJavaBigDecimal = JavaBigDecimal.valueOf(value)

  final def toBigDecimal: Result[BigDecimal] = (toJavaBigDecimal: BigDecimal).asRight
  final def toBigInt: Result[BigInt] = {
    val asBigDecimal = toJavaBigDecimal

    if (bigDecimalIsWhole(asBigDecimal)) new BigInt(asBigDecimal.toBigInteger).asRight
    else Error("Can't convert to BigInt").asLeft
  }

  final def toDouble: Double = value
  final def toFloat: Float   = value.toFloat

  final def toLong: Result[Long] = {
    val asBigDecimal = toJavaBigDecimal

    if (bigDecimalIsValidLong(asBigDecimal)) asBigDecimal.longValue.asRight else Error("Can't convert to BigInt").asLeft
  }
}
