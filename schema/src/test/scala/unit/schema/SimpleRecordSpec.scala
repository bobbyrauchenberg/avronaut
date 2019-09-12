package unit.schema

import common._

class SimpleRecordSpec extends UnitSpecBase {

  "schema" should {

    "be built for a basic null record" in new TestContext {
      schemaAsString[SimpleNull] should beRight(simpleSchema("SimpleNull", "null"))
    }

    "be built for a basic boolean record" in new TestContext {
      schemaAsString[SimpleBool] should beRight(simpleSchema("SimpleBool", "boolean"))
    }

    "be built for a basic string record" in new TestContext {
      schemaAsString[SimpleString] should beRight(simpleSchema("SimpleString", "string"))
    }

    "be built for a basic int record" in new TestContext {
      schemaAsString[SimpleInt] should beRight(simpleSchema("SimpleInt", "int"))
    }

    "be built for a basic long record" in new TestContext {
      schemaAsString[SimpleLong] should beRight(simpleSchema("SimpleLong", "long"))
    }

    "be built for a basic float record" in new TestContext {
      schemaAsString[SimpleFloat] should beRight(simpleSchema("SimpleFloat", "float"))
    }

    "be built for a basic double record" in new TestContext {
      schemaAsString[SimpleDouble] should beRight(simpleSchema("SimpleDouble", "double"))
    }

    "be built for a basic bytes record" in new TestContext {
      schemaAsString[SimpleByte] should beRight(simpleSchema("SimpleByte", "bytes"))
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

}
