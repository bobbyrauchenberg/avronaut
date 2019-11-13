package unit.codec

import cats.syntax.either._
import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
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
        runCodecRoundTrip[EitherUnion]
        runCodecRoundTrip[WriterUnionWithCaseClass]
        runCodecRoundTrip[UnionWithEitherOfOption]
        runCodecRoundTrip[UnionWithOptionalEither]
        runCodecRoundTrip[WriterRecordWithMap]
        runCodecRoundTrip[WriterRecordWithMapOfRecord]
        runCodecRoundTrip[WriterRecordWithList]
        runCodecRoundTrip[RecordWithSet]
        runCodecRoundTrip[RecordWithOptionUnion]
        runCodecRoundTrip[RecordWithOptionUnionOfCaseclass]
        runCodecRoundTrip[RecordWithOptionalListCaseClass]
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
  case class EitherUnion(field: Either[Boolean, Int])
  case class Cupcat(field1: Boolean, field2: Float)
  case class Rendal(field1: Boolean, field2: String)
  case class WriterUnionWithCaseClass(field1: Either[Cupcat, Rendal])
  case class UnionWithOptionalEither(field: Option[Either[Cupcat, Rendal]])
  case class UnionWithEitherOfOption(field: Either[Option[Cupcat], Either[Rendal, String]])
  case class UnionWithDefaultCaseClass(field: Either[Cupcat, Rendal] = Cupcat(true, 123.8f).asLeft)
  case class WriterRecordWithMap(writerField: String, field1: Map[String, Boolean], field2: Int)
  case class ReaderRecordWithMap(field1: Map[String, Boolean], field2: Int)
  case class WriterRecordWithMapOfRecord(field1: Int, writerField: Boolean, field2: Map[String, Nested])
  case class WriterRecordWithList(writerField: Boolean, field1: String, field2: Map[String, List[Int]])
  case class RecordWithOptionUnion(field: Option[String])
  case class RecordWithOptionUnionOfCaseclass(field: Option[Nested])
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])
  case class RecordWithSet(field: Set[Int])

  implicit val primitivesRecordCodec: Codec[PrimitivesRecord]                   = Codec[PrimitivesRecord]
  implicit val nestedCodec: Codec[Nested]                                       = Codec[Nested]
  implicit val recordWithListOfCaseClassCodec: Codec[RecordWithListOfCaseClass] = Codec[RecordWithListOfCaseClass]
  implicit val recordWithListOfEither: Codec[RecordWithListOfEither]            = Codec[RecordWithListOfEither]
  implicit val testRecordCodec: Codec[TestRecord]                               = Codec[TestRecord]
  implicit val nestedRecordCodec: Codec[NestedRecord]                           = Codec[NestedRecord]
  implicit val eitherUnionCodec: Codec[EitherUnion]                             = Codec[EitherUnion]
  implicit val writerUnionWithCaseClassEncoder: Codec[WriterUnionWithCaseClass] = Codec[WriterUnionWithCaseClass]
  implicit val unionWithOptionalEitherCodec: Codec[UnionWithOptionalEither]     = Codec[UnionWithOptionalEither]
  implicit val unionWithEitherOfOptionCodec: Codec[UnionWithEitherOfOption]     = Codec[UnionWithEitherOfOption]
  implicit val unionWithDefaultCaseClassCodec: Codec[UnionWithDefaultCaseClass] = Codec[UnionWithDefaultCaseClass]
  implicit val writeRecordWithMapCodec: Codec[WriterRecordWithMap]              = Codec[WriterRecordWithMap]
  implicit val writerRecordWithMapOfRecordCodec: Codec[WriterRecordWithMapOfRecord] =
    Codec[WriterRecordWithMapOfRecord]
  implicit val writerRecordWithListCodec: Codec[WriterRecordWithList]   = Codec[WriterRecordWithList]
  implicit val recordWithOptionUnionCodec: Codec[RecordWithOptionUnion] = Codec[RecordWithOptionUnion]
  implicit val recordWithOptionUnionOfCaseClassCodec: Codec[RecordWithOptionUnionOfCaseclass] =
    Codec[RecordWithOptionUnionOfCaseclass]
  implicit val unionWithListCodec: Codec[RecordWithOptionalListCaseClass] = Codec[RecordWithOptionalListCaseClass]
  implicit val codec: Codec[RecordWithSet]                                = Codec[RecordWithSet]

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
