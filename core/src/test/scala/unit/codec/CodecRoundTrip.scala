package unit.codec

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import unit.codec.SealedTraitRoundTripSpec.{XWrapper, YWrapper}
import unit.common.RunRoundTripAssert._
import unit.utils.UnitSpecBase

class CodecRoundTrip extends UnitSpecBase {

  "codec" should {
    "encode and decode a record" in {
      forAll { record: PrimitivesRecord =>
        runCodecRoundTrip[PrimitivesRecord]
        runCodecRoundTrip[NestedRecord]
        runCodecRoundTrip[XWrapper]
        runCodecRoundTrip[YWrapper]
        runCodecRoundTrip[TestRecord]
        runCodecRoundTrip[Nested]
        runCodecRoundTrip[RecordWithListOfCaseClass]
        runCodecRoundTrip[RecordWithListOfEither]
      }
    }

  }

  case class X(field: Either[Boolean, String])

  case class PrimitivesRecord(string: String,
                              boolean: Boolean,
                              int: Int,
                              float: Float,
                              double: Double,
                              long: Long,
                              bytes: Array[Byte])

  case class NestedRecord(string: String, boolean: Boolean, inner: Inner)
  case class Inner(value: String, value2: Int)
  case class TestRecord(field: List[String])
  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithListOfCaseClass(field: List[Nested])
  case class RecordWithListOfEither(field: List[Either[String, Boolean]])

  implicit val primitivesRecordCodec: Codec[PrimitivesRecord]                   = Codec[PrimitivesRecord]
  implicit val nestedCodec: Codec[Nested]                                       = Codec[Nested]
  implicit val recordWithListOfCaseClassCodec: Codec[RecordWithListOfCaseClass] = Codec[RecordWithListOfCaseClass]
  implicit val recordWithListOfEither: Codec[RecordWithListOfEither]            = Codec[RecordWithListOfEither]
  implicit val testRecordCodec: Codec[TestRecord]                               = Codec[TestRecord]
  implicit val nestedRecordCodec: Codec[NestedRecord]                           = Codec[NestedRecord]
}

object SealedTraitRoundTripSpec {

  sealed trait X
  case object A               extends X
  case class B(field: String) extends X
  case class XWrapper(field: X)

  implicit val codecX: Codec[XWrapper] = Codec[XWrapper]

  sealed trait Y
  case object Y1 extends Y
  case object Y2 extends Y
  case class YWrapper(field: Y)

  implicit val codecY: Codec[YWrapper] = Codec[YWrapper]
}
