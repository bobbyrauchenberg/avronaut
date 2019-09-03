package common

import cats.scalatest.EitherMatchers
import com.rauchenberg.cupcatAvro.schema.instances.schemaInstances
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends WordSpecLike with Matchers with EitherMatchers with schemaInstances

trait ProperyChecksSpecBase extends UnitSpecBase with ScalaCheckPropertyChecks
