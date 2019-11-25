package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.common._

class MapSpec extends UnitSpecBase {

  "schema" should {
    "build a record with a Map" in {
      implicit val schema = AvroSchema.toSchema[RecordWithMap]
      val expected =
        """{"type":"record","name":"RecordWithMap","namespace":"unit.schema.MapSpec",
          |"fields":[{"name":"cupcat","type":{"type":"map","values":"string"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[RecordWithMap] shouldBe expected
    }
    "build a record with a Map with a default" in {
      implicit val schema = AvroSchema.toSchema[RecordWithDefaultMap]
      val expected =
        """{"type":"record","name":"RecordWithDefaultMap","namespace":"unit.schema.MapSpec",
          |"fields":[{"name":"cupcat","type":{"type":"map","values":"string"},
          |"default":{"cup":"cat"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[RecordWithDefaultMap] shouldBe expected
    }
  }

  case class RecordWithMap(cupcat: Map[String, String])
  case class RecordWithDefaultMap(cupcat: Map[String, String] = Map("cup" -> "cat"))

}
