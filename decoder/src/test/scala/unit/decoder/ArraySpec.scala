package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import unit.common.UnitSpecBase
import unit.decoder.utils.RunAssert._

class ArraySpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a list" in {
      forAll { record: RecordWithList =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a seq" in {
      forAll { record: RecordWithSeq =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a vector" in {
      forAll { record: RecordWithSeq =>
        runAssert(record.field, record)
      }
    }
    "decode a record with a list and a default value" in {
      val record = RecordWithListDefault()
      runAssert(record.field, record)
    }
    "decode a record with a seq and a default value" in {
      val record = RecordWithSeqDefault()
      runAssert(record.field, record)
    }
    "decode a record with a vector and a default value" in {
      val record = RecordWithVectorDefault()
      runAssert(record.field, record)
    }
  }

  case class RecordWithList(field: List[String])
  case class RecordWithSeq(field: Seq[String])
  case class RecordWithVector(field: Vector[String])
  case class RecordWithListDefault(field: List[String] = List("cup", "cat"))
  case class RecordWithSeqDefault(field: Seq[String] = Seq("cup", "cat"))
  case class RecordWithVectorDefault(field: Vector[String] = Vector("cup", "cat"))


}
