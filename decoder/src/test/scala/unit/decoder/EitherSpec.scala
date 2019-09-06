package unit.decoder

import cats.syntax.either._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert._

class EitherSpec extends UnitSpecBase {

  "decoder" should {
    "decode a union of A and B into a Left[A]" in {
      forAll { v: String =>
        runAssert(v, Union(v.asLeft))
      }
    }
    "decode a union of A and B into a Right[B]" in {
      forAll { v: Boolean =>
        runAssert(v, Union(v.asRight))
      }
    }
  }

  case class Union(field: Either[String, Boolean])
}
