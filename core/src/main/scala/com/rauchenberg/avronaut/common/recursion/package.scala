package com.rauchenberg.avronaut.common.recursion

import cats.{Applicative, Functor}

/*
    Cats Traverse requires foldLeft / foldRight impls which I don't need here
 */
trait TraverseF[F[_]] extends Functor[F] {
  def traverse[G[_] : Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]
}

package object recursion {

  val Fix: FixModule = FixImpl
  type Fix[F[_]] = Fix.Fix[F]

  type FAlgebra[F[_], A]   = F[A] => A
  type FCoalgebra[F[_], A] = A => F[A]

  type FAlgebraM[M[_], F[_], A]   = F[A] => M[A]
  type FCoalgebraM[M[_], F[_], A] = A => M[F[A]]

  @inline implicit class FixOps[F[_]](private val self: Fix[F]) extends AnyVal {
    @inline def unfix: F[Fix[F]] =
      Fix.unfix(self)
  }

  implicit class TraverseFSyntax[F[_] : TraverseF, A](fa: F[A]) {
    def traverse[G[_] : Applicative, B](f: A => G[B]) =
      implicitly[TraverseF[F]].traverse(fa)(f)
  }

}
