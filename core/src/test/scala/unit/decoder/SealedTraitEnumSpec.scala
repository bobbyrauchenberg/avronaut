package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import unit.utils.RunAssert._
import unit.utils.UnitSpecBase

class SealedTraitSpec extends UnitSpecBase {

  import SealedTraitSpec._

  "decoder" should {

    "handle sealed trait enums" in {
      forAll { enumRecord: EnumRecord =>
        implicit val codec = Codec[EnumRecord]
        runDecodeAssert(enumRecord.field.toString, enumRecord)
      }
    }

    "handle sealed trait enums with defaults" in {
      implicit val codec = Codec[SealedTraitEnumWithDefault]
      runDecodeAssert(B.toString, SealedTraitEnumWithDefault())
    }
  }

}

private[this] object SealedTraitSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class EnumRecord(field: A)

  case class SealedTraitEnumWithDefault(field: A = B)

}
