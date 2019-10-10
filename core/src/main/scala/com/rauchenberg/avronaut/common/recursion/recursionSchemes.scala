package com.rauchenberg.avronaut.common.recursion

import com.rauchenberg.avronaut.common.recursion.recursion._
import scalaz.syntax.monad._
import scalaz.syntax.traverse._
import scalaz.{Functor, Monad, Traverse}

object Morphisms {

  def cata[F[_], A, B](fAlgebra: FAlgebra[F, A])(f: Fix[F])(implicit F: Functor[F]): A = {
    var self: Fix[F] => A = null
    self = f => fAlgebra(F.map(f.unfix)(self))
    fAlgebra(F.map(f.unfix)(cata(fAlgebra)))
  }

  def cataM[M[_] : Monad, F[_] : Traverse, A](fAlgebraM: FAlgebraM[M, F, A]): Fix[F] => M[A] = {
    var self: Fix[F] => M[A] = null
    self = f => f.unfix.traverse(self).flatMap(fAlgebraM)
    self
  }

  def ana[F[_], A](coalgebra: FCoalgebra[F, A])(implicit F: Functor[F]): A => Fix[F] = {
    var self: A => Fix[F] = null
    self = a => Fix[F](F.map(coalgebra(a))(self))
    self
  }

  def anaM[M[_], F[_], A](coalgebra: FCoalgebraM[M, F, A])(implicit M: Monad[M], F: Traverse[F]): A => M[Fix[F]] = {
    var self: A => M[Fix[F]] = null
    self = a => coalgebra(a).flatMap(fa => M.map(F.traverse(fa)(self))(Fix.apply[F]))
    self
  }

  def hyloM[M[_], F[_], A, B](coalg: FCoalgebraM[M, F, A], alg: FAlgebraM[M, F, B])(implicit M: Monad[M],
                                                                                    F: Traverse[F]): A => M[B] = {
    var self: A => M[B] = null
    self = a => M.bind(coalg(a))(fa => M.bind(F.traverse(fa)(self))(alg))
    self
  }

}
