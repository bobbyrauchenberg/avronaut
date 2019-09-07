package unit.decoder.utils

import org.scalacheck.{Arbitrary, Gen}
import shapeless.{:+:, CNil, Coproduct}
import shapeless.ops.coproduct.Inject

object ShapelessArbitraries {

  implicit def arbCoproduct[A : Arbitrary, B <: Coproduct](implicit
                                                           inj: Inject[A :+: B, A],
                                                           inj2: Inject[A :+: B, B],
                                                           arbA: Arbitrary[A],
                                                           arbB: Arbitrary[B]): Arbitrary[:+:[A, B]] =
    Arbitrary(Gen.oneOf(arbA.arbitrary.map(Coproduct[A :+: B](_)), arbB.arbitrary.map(Coproduct[A :+: B](_))))

  implicit def arb[A](implicit aa: Arbitrary[A]): Arbitrary[A :+: CNil] =
    Arbitrary(aa.arbitrary.map(Coproduct[A :+: CNil](_)))

}
