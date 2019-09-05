package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import unit.common.UnitSpecBase
import unit.decoder.RecordsWithArrays._
import unit.decoder.utils.RunAssert._

class ArraySpec extends UnitSpecBase {

  "decoder" should {
    "convert a record with a list" in {
      forAll { record: RecordWithList =>
        singleFieldAssertion(record.field, record)
      }
    }
    "convert a record with a seq" in {
      forAll { record: RecordWithSeq =>
        singleFieldAssertion(record.field, record)
      }
    }
    "convert a record with a vector" in {
      forAll { record: RecordWithSeq =>
        singleFieldAssertion(record.field, record)
      }
    }
    "convert a record with a list and a default value" in {
      val record = RecordWithListDefault()
      singleFieldAssertion(record.field, record)
    }
    "convert a record with a seq and a default value" in {
      val record = RecordWithSeqDefault()
      singleFieldAssertion(record.field, record)
    }
    "convert a record with a vector and a default value" in {
      val record = RecordWithVectorDefault()
      singleFieldAssertion(record.field, record)
    }
  }


}

private[this] object RecordsWithArrays {

  case class RecordWithList(field: List[String])

  case class RecordWithSeq(field: Seq[String])

  case class RecordWithVector(field: Vector[String])

  case class RecordWithListDefault(field: List[String] = List("cup", "cat"))

  case class RecordWithSeqDefault(field: Seq[String] = Seq("cup", "cat"))

  case class RecordWithVectorDefault(field: Vector[String] = Vector("cup", "cat"))

}