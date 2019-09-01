package common

import cats.scalatest.EitherMatchers
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


trait UnitSpecBase extends WordSpecLike with Matchers with EitherMatchers

trait ProperyChecksSpecBase extends UnitSpecBase with ScalaCheckPropertyChecks
