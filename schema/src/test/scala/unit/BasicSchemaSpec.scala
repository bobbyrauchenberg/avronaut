package unit

import com.rauchenberg.cupcat1.schema.{AvroSchema, SchemaError}
import com.rauchenberg.cupcat1.schema.instances._
import AvroSchema._
import common.ProperyChecksSpecBase
import org.scalacheck.{Arbitrary, Gen}

class BasicSchemaSpec extends ProperyChecksSpecBase {

  implicit val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  "schema" should {

    "be built for a basic null record" in new TestContext {
      schemaAsString[SimpleNull] should beRight(simpleSchema("null"))
    }

    "be built for a basic boolean record" in new TestContext {
      schemaAsString[SimpleBool] should beRight(simpleSchema("boolean"))
    }

    "be built for a basic string record" in new TestContext {
      schemaAsString[SimpleString] should beRight(simpleSchema("string"))
    }

    "be built for a basic int record" in new TestContext {
      schemaAsString[SimpleInt] should beRight(simpleSchema("int"))
    }

    "be built for a basic long record" in new TestContext {
      schemaAsString[SimpleLong] should beRight(simpleSchema("long"))
    }

    "be built for a basic float record" in new TestContext {
      schemaAsString[SimpleFloat] should beRight(simpleSchema("float"))
    }

    "be built for a basic double record" in new TestContext {
      schemaAsString[SimpleDouble] should beRight(simpleSchema("double"))
    }

    "be built for a basic bytes record" in new TestContext {
      schemaAsString[SimpleByte] should beRight(simpleSchema("bytes"))
    }
  }

  trait TestContext {

    def schemaAsString[T : AvroSchema] = AvroSchema[T].schema.map(_.toString)

    def simpleSchema(valueType: String) = s"""{"type":"record","fields":[{"name":"value","type":"$valueType"}]}"""
  }

}

case class SimpleNull(value: Null)
case class SimpleBool(value: Boolean)
case class SimpleString(value: String)
case class SimpleInt(value: Int)
case class SimpleLong(value: Long)
case class SimpleFloat(value: Float)
case class SimpleDouble(value: Double)
case class SimpleByte(value: Byte)

