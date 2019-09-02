package unit

import com.rauchenberg.cupcatAvro.schema.{AvroSchema, instances}
import common._
import AvroSchema._
import common.ProperyChecksSpecBase
import org.scalacheck.{Arbitrary, Gen}
import SimpleRecordDefaults._

class SimpleRecordDefaultsSchemaSpec extends ProperyChecksSpecBase with instances {

  implicit val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  "schema" should {

    "have a default for a basic boolean record" in new TestContext {
      schemaAsString[BoolWithDefault] should beRight(simpleSchema("BoolWithDefault", "boolean", true))
    }

    "have a default for a basic string record" in new TestContext {
        schemaAsString[StringWithDefault] should beRight(simpleSchema("StringWithDefault", "string", "\"cupcat\""))
    }

    "have a default for a basic int record" in new TestContext {
        schemaAsString[IntWithDefault] should beRight(simpleSchema("IntWithDefault", "int", 5))
    }

    "have a default for a basic long record" in new TestContext {
      schemaAsString[LongWithDefault] should beRight(simpleSchema("LongWithDefault", "long", 5l))
    }

    "have a default for a basic float record" in new TestContext {
      schemaAsString[FloatWithDefault] should beRight(simpleSchema("FloatWithDefault", "float", 5f))
    }

    "have a default for a basic double record" in new TestContext {
      schemaAsString[DoubleWithDefault] should beRight(simpleSchema("DoubleWithDefault", "double", 5d))
    }

    "error when trying to give a default for a bytes record" in new TestContext {
      schemaAsString[ByteWithDefault] should beRight(simpleSchema("ByteWithDefault", "bytes", "\"cupcat\""))
    }
  }

  trait TestContext {
    def simpleSchema[T](typeName: String, valueType: String, default: T) = s"""{"type":"record","name":"$typeName","doc":"","fields":[{"name":"value","type":"$valueType","doc":"","default":$default}]}"""
  }

}

private [this] object SimpleRecordDefaults {
  case class BoolWithDefault(value: Boolean = true)
  case class StringWithDefault(value: String = "cupcat")
  case class IntWithDefault(value: Int = 5)
  case class LongWithDefault(value: Long = 5L)
  case class FloatWithDefault(value: Float = 5.0f)
  case class DoubleWithDefault(value: Double = 5D)
  case class ByteWithDefault(value: Array[Byte] = "cupcat".getBytes)
}

