package unit.schema

import java.time.OffsetDateTime
import java.util.UUID

import common.{schemaAsString, UnitSpecBase}

class LogicalTypeSpec extends UnitSpecBase {

  "schema" should {

    "encode an OffsetDateTime" in {
      val expected =
        """
          |{"type":"record","name":"RecordWithDateTime","namespace":"unit.schema.LogicalTypeSpec","doc":"",
          |"fields":[{"name":"field","type":{"type":"long",
          |"logicalType":"timestamp-millis"}}]}""".stripMargin.replaceAll("\n", "")

      schemaAsString[RecordWithDateTime] should beRight(expected)
    }

    "encode a UUID" in {
      val expected =
        """{"type":"record","name":"RecordWithUUID","namespace":"unit.schema.LogicalTypeSpec","doc":"",
          |"fields":[{"name":"field","type":{"type":"string","logicalType":"uuid"}}]}""".stripMargin.replaceAll("\n",
                                                                                                                "")

      schemaAsString[RecordWithUUID] should beRight(expected)
    }
  }

  case class RecordWithDateTime(field: OffsetDateTime)
  case class RecordWithUUID(field: UUID)
}
