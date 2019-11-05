package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase

class SetSpec extends UnitSpecBase {

  "schema" should {
    "build a schema with a set" in {
      implicit val recordWithSetSchemaData = AvroSchema.toSchema[RecordWithSet]

      val expected =
        """{"type":"record","name":"RecordWithSet","namespace":"unit.schema.SetSpec","doc":"","fields":
          |[{"name":"s","type":{"type":"array","items":"int"}}]}""".stripMargin.replace("\n", "")

      schemaAsString[RecordWithSet] shouldBe expected
    }
  }

  case class RecordWithSet(s: Set[Int])

}
