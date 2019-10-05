package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import UserDefinedInstance._

class UserDefinedInstanceSpec extends UnitSpecBase {

  "schema" should {
    "be derivable using instances we don't provide but which are defined by a user" in {

      //won't compile if the UUID instance in UserDefinedInstances isn't found
      AvroSchema[unit.schema.UserDefinedInstance]
    }
    "allow overrides" in {

      val expected =
        """{"type":"record","name":"OverrideInt","namespace":"unit.schema","doc":"",
          |"fields":[{"name":"value","type":"string"}]}""".stripMargin.replace("\n", "")

      import OverrideInt._
      //uses a string schema type where normally an int would be used
      schemaAsString[OverrideInt] should beRight(expected)

    }
  }

}
