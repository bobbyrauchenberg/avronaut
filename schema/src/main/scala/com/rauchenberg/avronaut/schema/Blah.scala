package com.rauchenberg.avronaut.schema

import matryoshka._
import scalaz.Scalaz._
import scalaz._
import slamdata.Predef._

sealed abstract class Example[A]
final case class Empty[A]()                                   extends Example[A]
final case class NonRec[A](a: String, b: Int)                 extends Example[A]
final case class SemiRec[A](a: Int, b: A)                     extends Example[A]
final case class MultiRec[A](a: A, b: A)                      extends Example[A]
final case class OneList[A](l: List[A])                       extends Example[A]
final case class TwoLists[A](first: List[A], second: List[A]) extends Example[A]

object Example extends App {
  implicit val traverse: Traverse[Example] = new Traverse[Example] {
    def traverseImpl[G[_], A, B](fa: Example[A])(f: A => G[B])(implicit G: Applicative[G]): G[Example[B]] = fa match {
      case Empty()        => G.point(Empty())
      case NonRec(a, b)   => G.point(NonRec(a, b))
      case SemiRec(a, b)  => f(b).map(SemiRec(a, _))
      case MultiRec(a, b) => (f(a) ⊛ f(b))(MultiRec(_, _))
      case OneList(a)     => a.traverse(f) ∘ (OneList(_))
      case TwoLists(a, b) => (a.traverse(f) ⊛ b.traverse(f))(TwoLists(_, _))
    }
  }

  implicit val show: Delay[Show, Example] = new Delay[Show, Example] {
    def apply[α](s: Show[α]) = {
      implicit val is = s
      Show.show {
        case Empty() => Cord("Empty()")
        case NonRec(s2, i2) =>
          Cord("NonRec(" + s2.shows + ", " + i2.shows + ")")
        case SemiRec(i2, a2) =>
          Cord("SemiRec(") ++ i2.show ++ Cord(", ") ++ s.show(a2) ++ Cord(")")
        case MultiRec(a2, b2) =>
          Cord("MultiRec(") ++ s.show(a2) ++ Cord(", ") ++ s.show(b2) ++ Cord(")")
        case OneList(r) => Cord("OneList(") ++ r.show ++ Cord(")")
        case TwoLists(r1, r2) =>
          Cord("TwoLists(") ++ r1.show ++ Cord(", ") ++ r2.show ++ Cord(")")
      }
    }
  }

}
