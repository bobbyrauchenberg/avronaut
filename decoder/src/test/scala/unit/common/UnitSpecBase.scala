package unit.common

import cats.scalatest.EitherMatchers
import com.rauchenberg.cupcatAvro.decoder.instances.decoderInstances
import com.rauchenberg.cupcatAvro.schema.instances.schemaInstances
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends WordSpecLike with ScalaCheckPropertyChecks with Matchers with EitherMatchers with decoderInstances with schemaInstances
