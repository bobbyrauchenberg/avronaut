package unit.schema

import java.time.OffsetDateTime

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import unit.common._

class DateTimeSpec extends UnitSpecBase {

  "schema" should {
    "be able to encode dates" in {
      implicit val schema = AvroSchema.toSchema[DateTimeField]
      val expected        = """
        |{"type":"record","name":"DateTimeField","namespace":"unit.schema.DateTimeSpec"
        |,"fields":[{"name":"field","type":{"type":"long",
        |"logicalType":"timestamp-millis"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[DateTimeField] shouldBe expected
    }
  }

  case class DateTimeField(field: OffsetDateTime)

}
