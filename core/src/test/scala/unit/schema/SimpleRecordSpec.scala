package unit.schema

import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{Namespace, SchemaMetadata}
import unit.utils.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "schema" should {

    "be built for a basic null record" in new TestContext {
      schemaAsString[SimpleNull] shouldBe simpleSchema("SimpleNull", "null")
    }

    "be built for a basic boolean record" in new TestContext {
      schemaAsString[SimpleBool] shouldBe simpleSchema("SimpleBool", "boolean")
    }

    "be built for a basic string record" in new TestContext {
      schemaAsString[SimpleString] shouldBe simpleSchema("SimpleString", "string")
    }

    "be built for a basic int record" in new TestContext {
      schemaAsString[SimpleInt] shouldBe simpleSchema("SimpleInt", "int")
    }

    "be built for a basic long record" in new TestContext {
      schemaAsString[SimpleLong] shouldBe simpleSchema("SimpleLong", "long")
    }

    "be built for a basic float record" in new TestContext {
      schemaAsString[SimpleFloat] shouldBe simpleSchema("SimpleFloat", "float")
    }

    "be built for a basic double record" in new TestContext {
      schemaAsString[SimpleDouble] shouldBe simpleSchema("SimpleDouble", "double")
    }

    "be built for a basic bytes record" in new TestContext {
      schemaAsString[SimpleByte] shouldBe simpleSchema("SimpleByte", "bytes")
    }

  }

  trait TestContext {
    def simpleSchema(typeName: String, valueType: String) =
      s"""{"type":"record","name":"$typeName",
         |"namespace":"unit.schema.SimpleRecordSpec","doc":"",
         |"fields":[{"name":"value","type":"$valueType"}]}""".stripMargin.replace("\n", "")
  }

  case class SimpleNull(value: Null)
  case class SimpleBool(value: Boolean)
  case class SimpleString(value: String)
  case class SimpleStringWithDefault(value: String)
  case class SimpleInt(value: Int)
  case class SimpleLong(value: Long)
  case class SimpleFloat(value: Float)
  case class SimpleDouble(value: Double)
  case class SimpleByte(value: Array[Byte])

  case class Inner(field1: String)
  case class Nested(field1: Inner)
  case class WithNesting(field1: SimpleBool,
                         field2: Nested,
                         @SchemaMetadata(Map(Namespace -> "com.cupcat")) field3: SimpleBool)

}
