package com.rauchenberg.avronaut.common

import cats.{Applicative, Eval, Traverse}
import cats.syntax.traverse._
import cats.instances.list._
import com.rauchenberg.avronaut.common.recursion.{Fix, TraverseF}

sealed trait AvroF[+A]
final case object AvroNullF                            extends AvroF[Nothing]
final case class AvroIntF(value: Int)                  extends AvroF[Nothing]
final case class AvroLongF(value: Long)                extends AvroF[Nothing]
final case class AvroFloatF(value: Float)              extends AvroF[Nothing]
final case class AvroDoubleF(value: Double)            extends AvroF[Nothing]
final case class AvroBooleanF(value: Boolean)          extends AvroF[Nothing]
final case class AvroStringF(value: String)            extends AvroF[Nothing]
final case class AvroRecordF[A](value: List[A])        extends AvroF[A]
final case class AvroEnumF[B](value: String)           extends AvroF[Nothing]
final case class AvroUnionF[A](value: A)               extends AvroF[A]
final case class AvroArrayF[A](value: List[A])         extends AvroF[A]
final case class AvroMapF[A](value: List[(String, A)]) extends AvroF[A]
final case class AvroBytesF(value: Array[Byte])        extends AvroF[Nothing]
final case class AvroLogicalF[A](value: A)             extends AvroF[A]

object AvroF {
  type AvroFix = Fix[AvroF]

  implicit val AvroFTraverse = new TraverseF[AvroF] {
    override def traverse[G[_], A, B](fa: AvroF[A])(f: A => G[B])(implicit G: Applicative[G]): G[AvroF[B]] = fa match {
      case AvroNullF           => G.pure(AvroNullF)
      case a @ AvroIntF(_)     => G.pure(a)
      case a @ AvroLongF(_)    => G.pure(a)
      case a @ AvroFloatF(_)   => G.pure(a)
      case a @ AvroDoubleF(_)  => G.pure(a)
      case a @ AvroBooleanF(_) => G.pure(a)
      case a @ AvroStringF(_)  => G.pure(a)
      case AvroRecordF(value)  => G.map(value.traverse(f))(AvroRecordF(_))
      case a @ AvroEnumF(_)    => G.pure(a)
      case AvroUnionF(value)   => G.map(f(value))(AvroUnionF(_))
      case AvroArrayF(value)   => G.map(value.traverse(f))(AvroArrayF(_))
      case AvroMapF(value) => {
        G.map(value.map {
          case (s, a) =>
            G.map(f(a))(v => (s, v))
        }.sequence)(AvroMapF(_))
      }
      case a @ AvroBytesF(value) => G.pure(a)
      case AvroLogicalF(value)   => G.map(f(value))(AvroLogicalF(_))
    }

    override def map[A, B](fa: AvroF[A])(f: A => B): AvroF[B] = ???
  }
}
