package unit.schema

import java.time.OffsetDateTime

import unit.utils.UnitSpecBase

class DateTimeSpec extends UnitSpecBase {

  "schema" should {
    "be able to encode dates" in {

      val expected = """
        |{"type":"record","name":"DateTimeField","namespace":"unit.schema.DateTimeSpec"
        |,"doc":"","fields":[{"name":"field","type":{"type":"long",
        |"logicalType":"timestamp-millis"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[DateTimeField] shouldBe expected
    }
  }

  case class DateTimeField(field: OffsetDateTime)

}
