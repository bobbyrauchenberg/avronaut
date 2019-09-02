package unit

import common._
import common.UnitSpecBase
import NestedRecords.Nested

class NestedRecordsSpec extends UnitSpecBase {

  "schema" should {
    "handle nested records" in {
      val expected =
        """
          |{"type":"record","name":"Nested","namespace":"unit.NestedRecords","doc":"","fields":[{"name":"cupcat","type":"string","doc":""},
          |{"name":"bool","type":{"type":"record","name":"StringWithDefault","doc":"",
          |"fields":[{"name":"value","type":"string","doc":"","default":"cupcat"}]},"doc":""}]}""".stripMargin.replaceAll("\n", "")
      schemaAsString[Nested] should beRight(expected)
    }
  }
}

private [this] object NestedRecords {

  case class StringWithDefault(value: String = "cupcat")

  case class Nested(val cupcat: String, bool: StringWithDefault)
}
