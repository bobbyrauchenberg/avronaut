package common

import cats.scalatest.{EitherMatchers, EitherValues}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends WordSpecLike with Matchers with EitherMatchers with EitherValues with ScalaCheckPropertyChecks
