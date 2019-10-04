package com.rauchenberg.avronaut.schema

import com.rauchenberg.avronaut.common.Avro

import collection.JavaConverters._
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import matryoshka.patterns._
import matryoshka.patterns.EnvT
import org.apache.avro.generic.GenericData
import scalaz.Scalaz._
import scalaz.{Coproduct => _, _}
import shapeless._
import shapeless.:+:
import shapeless.ops.adjoin.Adjoin

sealed trait AvroT
case class AvroTInt(value: Int)                      extends AvroT
case class AvroTBool(value: Boolean)                 extends AvroT
case class AvroTString(value: String)                extends AvroT
case class AvroTArray(value: List[AvroT])            extends AvroT
case class AvroTRecord(value: List[AvroT])           extends AvroT
case class AvroTMap[T](value: List[(String, AvroT)]) extends AvroT

sealed trait AvroR[A]
case class AvroRInt[A](value: Int)            extends AvroR[A]
case class AvroRBool[A](value: Boolean)       extends AvroR[A]
case class AvroRString[A](value: String)      extends AvroR[A]
case class AvroRArray[A](value: List[A])      extends AvroR[A]
case class AvroRRecord[A](value: List[A])     extends AvroR[A]
case class AvroRMap[A](value: Map[String, A]) extends AvroR[A]

object RecursionScheme extends App {

  import matryoshka._
  import matryoshka.implicits._
  import shapeless.ops.adjoin._

  implicit val avroFunctorImpl: scalaz.Functor[AvroR] = new Functor[AvroR] {
    override def map[A, B](fa: AvroR[A])(f: A => B): AvroR[B] = fa match {
      case AvroRInt(v)    => AvroRInt(v)
      case AvroRString(v) => AvroRString(v)
      case AvroRBool(v)   => AvroRBool(v)
      case AvroRArray(v)  => AvroRArray(v.map(f))
      case AvroRMap(v)    => AvroRMap(v.mapValues(f))
      case AvroRRecord(v) => AvroRRecord(v.map(f))
    }
  }

  val avroCoalgebra: Coalgebra[AvroR, AvroT] = {
    case AvroTInt(value)    => AvroRInt(value)
    case AvroTBool(value)   => AvroRBool(value)
    case AvroTString(value) => AvroRString(value)
    case AvroTArray(value)  => AvroRArray(value)
    case AvroTMap(value)    => AvroRMap(value.toMap)
    case AvroTRecord(value) => AvroRRecord(value)
  }

  val avroAlgebra: Algebra[AvroR, Any] = {
    case AvroRInt(value) =>
      value.toString
    case AvroRBool(value) =>
      value.toString
    case AvroRString(value) =>
      value.toUpperCase
    case AvroRArray(value) =>
      value.asJava
    case AvroRMap(value) =>
      value.asJava
    case AvroRRecord(value) =>
      value
  }

  val zygoAlgebra: AvroR[Double] => Double = {
    case AvroRInt(value) =>
      1
    case AvroRBool(value) =>
      2
    case AvroRString(value) =>
      3
    case AvroRArray(value) =>
      4
    case AvroRMap(value) =>
      5
    case AvroRRecord(value) =>
      6
  }

  val avroGAlgebra: GAlgebra[(Double, ?), AvroR, Any] = {
    case AvroRInt(value) =>
      value.toString
    case AvroRBool(value) =>
      value.toString
    case AvroRString(value) =>
      value.toUpperCase
    case AvroRArray(value) =>
      value.asJava
    case AvroRMap(value) =>
      value.asJava
    case AvroRRecord(value) =>
      value
  }

  val avro: AvroT = AvroTRecord(
    List(
      AvroTInt(123),
      AvroTInt(456),
      AvroTMap(List(("cup" -> AvroTString("cat")), ("ren" -> AvroTString("dal")))),
      AvroTArray(List(AvroTArray(List(AvroTBool(true), AvroTBool(false))))),
      AvroTRecord(List(AvroTString("cupcat")))
    ))

  val anaResult      = avro.ana[Fix[AvroR]](avroCoalgebra)
  val cataResult     = anaResult.cata[Any](avroAlgebra)
  val cataResultPara = anaResult.zygo(zygoAlgebra, avroGAlgebra)

}
