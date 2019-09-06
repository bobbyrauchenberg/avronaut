package unit.decoder

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert.{beRight, _}

class EitherSpec extends UnitSpecBase {

  "decoder" should {
    "decode a union of A and B into a Left[A]" in  {
      forAll { v: String =>
        runAssert(v, Union(v.asRight))
      }
    }
    "decode a union of A and B into a Right[B]" in {
      forAll { v: Boolean =>
        runAssert(true, Union(true.asLeft))
      }
    }
    "always try to decode string second " in {
      forAll { v: Boolean =>
        runAssert(true, UnionWithStringFirst(true.asRight))
      }
    }
  }


  case class Union(field: Either[Boolean, String])
  case class UnionWithStringFirst(field: Either[String, Boolean])
}
