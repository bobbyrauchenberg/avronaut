package com.rauchenberg.avronaut.common

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

}
