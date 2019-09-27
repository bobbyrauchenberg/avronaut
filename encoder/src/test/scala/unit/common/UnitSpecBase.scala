package unit.common

import cats.scalatest.{EitherMatchers, EitherValues}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase
    extends WordSpecLike
    with ScalaCheckPropertyChecks
    with Matchers
    with EitherMatchers
    with EitherValues
