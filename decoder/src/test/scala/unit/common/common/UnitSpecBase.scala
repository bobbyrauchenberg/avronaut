package unit.common.common

import cats.scalatest.EitherMatchers
import com.rauchenberg.cupcatAvro.decoder.instances.decoderInstances
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends WordSpecLike with ScalaCheckPropertyChecks with Matchers with EitherMatchers with decoderInstances
