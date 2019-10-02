package com.rauchenberg.avronaut.schema

import collection.JavaConverters._
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import matryoshka.patterns._
import matryoshka.patterns.EnvT
import scalaz.Scalaz._
import scalaz._

sealed trait AvroT
case class AvroTInt(value: Int)            extends AvroT
case class AvroTBool(value: Boolean)       extends AvroT
case class AvroTString(value: String)      extends AvroT
case class AvroTArray(value: List[AvroT])  extends AvroT
case class AvroTRecord(value: List[AvroT]) extends AvroT

sealed trait AvroR[T]
case class AvroRInt[T](value: Int)        extends AvroR[T]
case class AvroRBool[T](value: Boolean)   extends AvroR[T]
case class AvroRString[T](value: String)  extends AvroR[T]
case class AvroRArray[T](value: List[T])  extends AvroR[T]
case class AvroRRecord[T](value: List[T]) extends AvroR[T]

object RecursionScheme extends App {

  import matryoshka._
  import matryoshka.implicits._

  implicit val avroFunctorImpl: scalaz.Functor[AvroR] = new Functor[AvroR] {
    override def map[A, B](fa: AvroR[A])(f: A => B): AvroR[B] = fa match {
      case AvroRInt(v)    => AvroRInt(v)
      case AvroRString(v) => AvroRString(v)
      case AvroRBool(v)   => AvroRBool(v)
      case AvroRArray(v)  => AvroRArray(v.map(f))
      case AvroRRecord(v) => AvroRRecord(v.map(f))
    }
  }

  val avroCoalgebra: Coalgebra[AvroR, AvroT] = {
    case AvroTInt(value)    => AvroRInt(value)
    case AvroTBool(value)   => AvroRBool(value)
    case AvroTString(value) => AvroRString(value)
    case AvroTArray(value)  => AvroRArray(value)
    case AvroTRecord(value) => AvroRRecord(value)
  }

  val avroAlgebra: Algebra[AvroR, Any] = {
    case AvroRInt(value)    => value.toString
    case AvroRBool(value)   => value.toString
    case AvroRString(value) => value.toUpperCase
    case AvroRArray(value)  => value.asJava
    case AvroRRecord(value) => value.map(_.toString.reverse)
  }

  val avro: AvroT = AvroTRecord(
    List(AvroTInt(123),
         AvroTInt(456),
         AvroTArray(List(AvroTArray(List(AvroTBool(true), AvroTBool(false))))),
         AvroTRecord(List(AvroTString("cupcat")))))

  val anaResult  = avro.ana[Fix[AvroR]](avroCoalgebra)
  val cataResult = anaResult.cata[Any](avroAlgebra)

  println(anaResult)
  println(cataResult)

}
