package unit.common

import cats.scalatest.{EitherMatchers, EitherValues}
import com.rauchenberg.cupcatAvro.encoder.instances.encoderInstances
import com.rauchenberg.cupcatAvro.schema.instances.schemaInstances
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends WordSpecLike with ScalaCheckPropertyChecks with Matchers
  with EitherMatchers with EitherValues with encoderInstances with schemaInstances
