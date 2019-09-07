package unit.decoder


import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import shapeless.ops.coproduct.Inject
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}
import unit.decoder.utils.RunAssert.runAssert
import utils.ShapelessArbitraries._
import unit.common.UnitSpecBase

class CoproductSpec extends UnitSpecBase {

  "decoder" should {

    "decode a union expressed with a coproduct 1" in {
      forAll { b: Boolean =>
        runAssert(b, Union(Coproduct[Boolean :+: Int :+: String :+: CNil](b)))
      }
    }

    "decode a union expressed with a coproduct 2" in {
      forAll { b: Int =>
        runAssert(b, Union(Coproduct[Boolean :+: Int :+: String :+: CNil](b)))
      }
    }

    "decode a union expressed with a coproduct 3" in {
      forAll { b: String =>
        runAssert(b, Union(Coproduct[Boolean :+: Int :+: String :+: CNil](b)))
      }
    }


  }

  case class Union(field: Boolean :+: Int :+: String :+: CNil)

}
