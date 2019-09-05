package unit.schema

import common._
import common.UnitSpecBase
import RecordsWithMaps._

class MapSpec extends UnitSpecBase {

  "schema" should {
    "build a record with a Map" in {
      val expected =
        """{"type":"record","name":"RecordWithMap","namespace":"unit.RecordsWithMaps",
          |"doc":"","fields":[{"name":"cupcat",
          |"type":{"type":"map","values":"string"}}]}""".stripMargin.replace("\n","")
      schemaAsString[RecordWithMap] should beRight(expected)
    }
    "build a record with a Map with a default" in {
      val expected =
        """{"type":"record","name":"RecordWithDefaultMap","namespace":"unit.RecordsWithMaps","doc":"",
          |"fields":[{"name":"cupcat","type":{"type":"map","values":"string"},
          |"doc":"","default":{"cup":"cat"}}]}""".stripMargin.replace("\n","")
      schemaAsString[RecordWithDefaultMap] should beRight(expected)
    }
  }

}

private [this] object RecordsWithMaps {

  case class RecordWithMap(cupcat: Map[String, String])

  case class RecordWithDefaultMap(cupcat: Map[String, String] = Map("cup" -> "cat"))

}
