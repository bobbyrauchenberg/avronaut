package unit

import common._
import common.UnitSpecBase
import RecordsWithMaps._

class MapSpec extends UnitSpecBase {

  "schema" should {
    "build a record with a Map of Strings" in {
      val expected =
        """{"type":"record","name":"RecordWithStringMap","namespace":"unit.RecordsWithMaps",
          |"doc":"","fields":[{"name":"cupcat",
          |"type":{"type":"map","values":"string"},"doc":""}]}""".stripMargin.replace("\n","")
      schemaAsString[RecordWithStringMap] should beRight(expected)

    }
  }

}

private [this] object RecordsWithMaps {

  case class RecordWithStringMap(cupcat: Map[String, String])
}
