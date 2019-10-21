package com.rauchenberg.avronaut.common

import scalaz._
import Scalaz._
import matryoshka.data.Fix
import org.apache.avro.Schema

sealed trait AvroF[+A]
final case object AvroNullF                                                    extends AvroF[Nothing]
final case class AvroIntF(value: Int)                                          extends AvroF[Nothing]
final case class AvroLongF(value: Long)                                        extends AvroF[Nothing]
final case class AvroFloatF(value: Float)                                      extends AvroF[Nothing]
final case class AvroDoubleF(value: Double)                                    extends AvroF[Nothing]
final case class AvroBooleanF(value: Boolean)                                  extends AvroF[Nothing]
final case class AvroStringF(value: String)                                    extends AvroF[Nothing]
final case class AvroEnumF[B](value: String)                                   extends AvroF[Nothing]
final case class AvroUnionF[A](value: A)                                       extends AvroF[A]
final case class AvroArrayF[@specialized A](value: List[A])                    extends AvroF[A]
final case class AvroPrimitiveArrayF[@specialized B](value: java.util.List[B]) extends AvroF[Nothing]
final case class AvroMapF[A](value: List[(String, A)])                         extends AvroF[A]
final case class AvroBytesF(value: Array[Byte])                                extends AvroF[Nothing]
final case class AvroLogicalF[A](value: A)                                     extends AvroF[A]
final case class AvroRecordF[A](schema: Schema, value: List[A])                extends AvroF[A]
final case class AvroRootF[A](schema: Schema, value: List[A])                  extends AvroF[A]
final case class AvroErrorF(msg: String)                                       extends AvroF[Nothing]

object AvroF {
  type AvroFix = Fix[AvroF]

  implicit val AvroFTraverse = new Traverse[AvroF] {
    override def traverseImpl[G[_], A, B](fa: AvroF[A])(f: A => G[B])(implicit G: Applicative[G]): G[AvroF[B]] =
      fa match {
        case AvroNullF                  => G.pure(AvroNullF)
        case a @ AvroIntF(_)            => G.pure(a)
        case a @ AvroLongF(_)           => G.pure(a)
        case a @ AvroFloatF(_)          => G.pure(a)
        case a @ AvroDoubleF(_)         => G.pure(a)
        case a @ AvroBooleanF(_)        => G.pure(a)
        case a @ AvroStringF(_)         => G.pure(a)
        case a @ AvroEnumF(_)           => G.pure(a)
        case a @ AvroPrimitiveArrayF(_) => G.pure(a)
        case AvroUnionF(value)          => G.map(f(value))(AvroUnionF(_))
        case AvroArrayF(value)          => G.map(value.traverse(f))(AvroArrayF(_))
        case AvroMapF(value) => {
          G.map(value.traverse {
            case (s, a) => G.map(f(a))(v => (s, v))
          })(AvroMapF(_))
        }
        case a @ AvroBytesF(_)          => G.pure(a)
        case AvroLogicalF(value)        => G.map(f(value))(AvroLogicalF(_))
        case AvroRecordF(schema, value) => G.map(value.traverse(f))(AvroRecordF(schema, _))
        case AvroRootF(schema, value)   => G.map(value.traverse(f))(AvroRootF(schema, _))
        case a @ AvroErrorF(_)          => G.pure(a)
      }

    override def map[A, B](fa: AvroF[A])(f: A => B): AvroF[B] = super.map(fa)(f)

  }
}
