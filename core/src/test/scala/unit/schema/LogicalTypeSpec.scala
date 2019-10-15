package unit.schema

import java.time.OffsetDateTime
import java.util.UUID

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase

class LogicalTypeSpec extends UnitSpecBase {

  "schema" should {

    "encode an OffsetDateTime" in {
      implicit val schema = AvroSchema.toSchema[RecordWithDateTime]
      val expected =
        """
          |{"type":"record","name":"RecordWithDateTime","namespace":"unit.schema.LogicalTypeSpec","doc":"",
          |"fields":[{"name":"field","type":{"type":"long",
          |"logicalType":"timestamp-millis"}}]}""".stripMargin.replaceAll("\n", "")

      schemaAsString[RecordWithDateTime] shouldBe expected
    }

    "encode a UUID" in {
      implicit val schema = AvroSchema.toSchema[RecordWithUUID]
      val expected =
        """{"type":"record","name":"RecordWithUUID","namespace":"unit.schema.LogicalTypeSpec","doc":"",
          |"fields":[{"name":"field","type":{"type":"string","logicalType":"uuid"}}]}""".stripMargin.replaceAll("\n",
                                                                                                                "")

      schemaAsString[RecordWithUUID] shouldBe expected
    }

  }

  case class RecordWithDateTime(field: OffsetDateTime)
  case class RecordWithUUID(field: UUID)

}
