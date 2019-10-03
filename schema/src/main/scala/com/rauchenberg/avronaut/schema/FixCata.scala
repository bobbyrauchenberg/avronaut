//package com.rauchenberg.avronaut.schema
//
//import com.rauchenberg.avronaut.schema.FixPoint.{FAlgebra, Fix}
//import com.rauchenberg.avronaut.schema.IntListF.IntList
//
//trait Functor[F[_]] {
//  def map[A, B](fa: F[A])(f: A => B): F[B]
//}
//
//object Functor {
//
//  implicit class MapSyntax[A, F[_]](val fa: F[A]) extends AnyVal {
//    def map[B](f: A => B)(implicit F: Functor[F]) = F.map(fa)(f)
//  }
//}
//
//sealed trait IntListF[+F]
//final case class IntCons[+F](head: Int, tail: F) extends IntListF[F]
//case object IntNil                               extends IntListF[Nothing]
//
//object IntListF {
//
//  type IntList = Fix[IntListF]
//
//  implicit val functor: Functor[IntListF] = new Functor[IntListF] {
//    override def map[A, B](fa: IntListF[A])(f: A => B): IntListF[B] = fa match {
//      case IntCons(head, tail) => IntCons(head, f(tail))
//      case IntNil              => IntNil
//    }
//  }
//
//}
//
//object IntList {
//
//  def apply(f: IntListF[IntList]): IntList = Fix(f)
//
//  def nil: IntList = apply(IntNil)
//
//  def cons(head: Int, tail: IntList): IntList = apply(IntCons(head, tail))
//
//  def fromList(is: Int*): IntList = is.foldRight(nil)(cons)
//
//  val listSum: FAlgebra[IntListF, Int] = {
//    case IntCons(h, t) => h + t
//    case IntNil        => 0
//  }
//
//  def sumList(list: IntList): Int = FixPoint.cata(listSum)(list)
//}
//
//sealed trait BinaryTreeF[+A, +F]
//final case class Node[+A, +F](left: F, value: A, right: F) extends BinaryTreeF[A, F]
//case object Leaf                                           extends BinaryTreeF[Nothing, Nothing]
//
//object BinaryTreeF {
//
//  type BinaryTree[A] = Fix[BinaryTreeF[A, ?]]
//
//  implicit def functor[A]: Functor[BinaryTreeF[A, ?]] = new Functor[BinaryTreeF[A, ?]] {
//
//    override def map[B, C](fa: BinaryTreeF[A, B])(f: B => C): BinaryTreeF[A, C] = fa match {
//      case Node(left, value, right) => Node(f(left), value, f(right))
//      case Leaf                     => Leaf
//    }
//
//  }
//
//}
//
//sealed trait JsonF[+F]
//case object Null                                   extends JsonF[Nothing]
//final case class Bool(value: Boolean)              extends JsonF[Nothing]
//final case class Str(value: String)                extends JsonF[Nothing]
//final case class Num(value: Double)                extends JsonF[Nothing]
//final case class Arr[+F](value: List[F])           extends JsonF[F]
//final case class Obj[+F](value: List[(String, F)]) extends JsonF[F]
//
//object JsonF {
//
//  type Json = Fix[JsonF]
//
//  implicit val functor: Functor[JsonF] = new Functor[JsonF] {
//    override def map[A, B](fa: JsonF[A])(f: A => B): JsonF[B] = fa match {
//      case Null        => Null
//      case j: Bool     => j
//      case j: Str      => j
//      case j: Num      => j
//      case Arr(values) => Arr(values.map(f))
//      case Obj(fields) => Obj(fields.map { case (k, v) => (k, f(v)) })
//
//    }
//  }
//
//}
//
//object FixPoint {
//
//  import Functor._
//
//  type FAlgebra[F[_], A] = F[A] => A
//
//  case class Fix[F[_]](unfix: F[Fix[F]])
//
//  def cata[F[_], A, B](fAlgebra: FAlgebra[F, A])(f: Fix[F])(implicit F: Functor[F]): A = {
//    var self: Fix[F] => A = null
//    self = f => fAlgebra(F.map(f.unfix))
//    fAlgebra(f.unfix.map(cata(fAlgebra)))
//  }
//
//}
