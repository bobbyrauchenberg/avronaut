package unit

import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaError}
import com.rauchenberg.cupcatAvro.schema.instances._
import common._
import AvroSchema._
import common.ProperyChecksSpecBase
import org.scalacheck.{Arbitrary, Gen}
import SimpleDefaultCases._

class DefaultSchemaSpec extends ProperyChecksSpecBase {

  implicit val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  "schema" should {

    "have a default for a basic boolean record" in new TestContext {
      schemaAsString[BoolWithDefault] should beRight(simpleSchema("boolean", true))
    }

    "have a default for a basic string record" in new TestContext {
        schemaAsString[StringWithDefault] should beRight(simpleSchema("string", "\"cupcat\""))
    }

    "have a default for a basic int record" in new TestContext {
        schemaAsString[IntWithDefault] should beRight(simpleSchema("int", 5))
    }

    "have a default for a basic long record" in new TestContext {
      schemaAsString[LongWithDefault] should beRight(simpleSchema("long", 5l))
    }

    "have a default for a basic float record" in new TestContext {
      schemaAsString[FloatWithDefault] should beRight(simpleSchema("float", 5f))
    }

    "have a default for a basic double record" in new TestContext {
      schemaAsString[DoubleWithDefault] should beRight(simpleSchema("double", 5d))
    }

    "error when trying to give a default for a bytes record" in new TestContext {
      schemaAsString[ByteWithDefault] should beLeft(SchemaError("Unknown datum class: class java.lang.Byte"))
    }
  }

  trait TestContext {
    def simpleSchema[T](valueType: String, default: T) = s"""{"type":"record","fields":[{"name":"value","type":"$valueType","doc":"","default":$default}]}"""
  }

}

object SimpleDefaultCases {
  case class BoolWithDefault(value: Boolean = true)
  case class StringWithDefault(value: String = "cupcat")
  case class IntWithDefault(value: Int = 5)
  case class LongWithDefault(value: Long = 5L)
  case class FloatWithDefault(value: Float = 5.0f)
  case class DoubleWithDefault(value: Double = 5D)
  case class ByteWithDefault(value: Byte = 'a'.toByte)
}

