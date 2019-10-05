package com.rauchenberg.avronaut.common.recursion

import cats.syntax.functor._
import cats.{Functor, Monad}
import cats.syntax.flatMap._
import com.rauchenberg.avronaut.common.recursion.recursion._

object Morphisms {

  def cata[F[_] : Functor, A, B](fAlgebra: FAlgebra[F, A])(f: Fix[F]): A = {
    var self: Fix[F] => A = null
    self = f => fAlgebra(f.unfix.map(self))
    fAlgebra(f.unfix.map(cata(fAlgebra)))
  }

  def cataM[M[_] : Monad, F[_] : TraverseF, A](fAlgebraM: FAlgebraM[M, F, A]): Fix[F] => M[A] = {
    var self: Fix[F] => M[A] = null
    self = f => f.unfix.traverse(self).flatMap(fAlgebraM)
    self
  }

  def ana[F[_], A](coalgebra: FCoalgebra[F, A])(implicit F: Functor[F]): A => Fix[F] = {
    var self: A => Fix[F] = null
    self = a => Fix[F](F.map(coalgebra(a))(self))
    self
  }

  def anaM[M[_], F[_], A](coalgebra: FCoalgebraM[M, F, A])(implicit M: Monad[M], F: TraverseF[F]): A => M[Fix[F]] = {
    var self: A => M[Fix[F]] = null
    self = a => M.flatMap(coalgebra(a))(fa => M.map(F.traverse(fa)(self))(Fix.apply[F]))
    self
  }

  def zipM[M[_], F[_], A, B](z1: FAlgebraM[M, F, A], z2: FAlgebraM[M, F, B])(implicit M: Monad[M],
                                                                             F: TraverseF[F]): FAlgebraM[M, F, (A, B)] =
    fa => {
      M.map2(z1(fa.map(_._1)), z2(fa.map(_._2))) { case (a, b) => (a, b) }
    }

}
