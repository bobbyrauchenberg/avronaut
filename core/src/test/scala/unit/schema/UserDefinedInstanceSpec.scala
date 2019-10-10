package unit.schema

import unit.schema.OverrideInt._
import unit.utils.UnitSpecBase

class UserDefinedInstanceSpec extends UnitSpecBase {

  "schema" should {
    "allow overrides" in {

      val expected =
        """{"type":"record","name":"OverrideInt","namespace":"unit.schema","doc":"",
          |"fields":[{"name":"value","type":"string"}]}""".stripMargin.replace("\n", "")

      //uses a string schema type where normally an int would be used
      schemaAsString[OverrideInt] shouldBe expected

    }
  }

}
