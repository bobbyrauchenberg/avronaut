package unit.schema

import common.UnitSpecBase
import common._
import com.rauchenberg.avronaut.schema.AvroSchema

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
    "treat a List as an Array with default" in new TestContext {
      runAssertWithDefault[RecordWithListDefault]("RecordWithListDefault")
    }
    "treat a Seq as an Array with default" in new TestContext {
      runAssertWithDefault[RecordWithSeqDefault]("RecordWithSeqDefault")
    }
    "treat a Vector as an Array with default" in new TestContext {
      runAssertWithDefault[RecordWithVectorDefault]("RecordWithVectorDefault")
    }
  }

  trait TestContext {
    def runAssert[T : AvroSchema](name: String) =
      schemaAsString[T] should beRight(
        s"""
           |{"type":"record","name":"$name","namespace":"unit.schema.ArraySpec","doc":"",
           |"fields":[{"name":"cupcat","type":{"type":"array","items":"string"}}]}
           |""".stripMargin.replace("\n", "")
      )
    def runAssertWithDefault[T : AvroSchema](name: String) =
      schemaAsString[T] should beRight(
        s"""
           |{"type":"record","name":"$name","namespace":"unit.schema.ArraySpec","doc":"",
           |"fields":[{"name":"cupcat","type":{"type":"array","items":"string"},"doc":"","default":["cup","cat"]}]}
           |""".stripMargin.replace("\n", "")
      )
  }

  case class RecordWithList(cupcat: List[String])
  case class RecordWithSeq(cupcat: Seq[String])
  case class RecordWithVector(cupcat: Vector[String])

  case class RecordWithListDefault(cupcat: List[String] = List("cup", "cat"))
  case class RecordWithSeqDefault(cupcat: Seq[String] = Seq("cup", "cat"))
  case class RecordWithVectorDefault(cupcat: Vector[String] = Vector("cup", "cat"))
}
