package unit

import common.UnitSpecBase
import common._
import RecordsWithArrays._
import com.rauchenberg.cupcatAvro.schema.AvroSchema

class ArraySpec extends UnitSpecBase {

  "schema" should {
    "treat a List as an Array" in new TestContext {
      runAssert[RecordWithList]("RecordWithList")
    }
    "treat a Seq as an Array" in new TestContext {
      runAssert[RecordWithSeq]("RecordWithSeq")
    }
    "treat a Vector as an Array" in new TestContext {
      runAssert[RecordWithVector]("RecordWithVector")
    }
  }

  trait TestContext {
    def runAssert[T : AvroSchema](name: String) =
      schemaAsString[T] should beRight(
        s"""{"type":"record","name":"$name","namespace":"unit.RecordsWithArrays","doc":"",
           |"fields":[{"name":"cupcat","type":{"type":"array","items":"string"},"doc":""}]}
           |""".stripMargin.replace("\n","")
      )
  }

}

private[this] object RecordsWithArrays {

  case class RecordWithList(cupcat: List[String])
  case class RecordWithSeq(cupcat: Seq[String])
  case class RecordWithVector(cupcat: Vector[String])

}
