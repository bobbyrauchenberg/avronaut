package unit

import com.rauchenberg.cupcatAvro.schema.AvroSchema
import common._
import AvroSchema._
import org.scalacheck.{Arbitrary, Gen}
import SimpleRecordDefaults._

class SimpleRecordDefaultsSchemaSpec extends UnitSpecBase {

  implicit val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  "schema" should {

    "have a default for a basic boolean record" in new TestContext {
      runAssert[BoolWithDefault, Boolean]("BoolWithDefault", "boolean", true)
    }

    "have a default for a basic string record" in new TestContext {
        runAssert[StringWithDefault, String]("StringWithDefault", "string", "\"cupcat\"")
    }

    "have a default for a basic int record" in new TestContext {
        runAssert[IntWithDefault, Int]("IntWithDefault", "int", 5)
    }

    "have a default for a basic long record" in new TestContext {
      runAssert[LongWithDefault, Long]("LongWithDefault", "long", 5l)
    }

    "have a default for a basic float record" in new TestContext {
      runAssert[FloatWithDefault, Float]("FloatWithDefault", "float", 5f)
    }

    "have a default for a basic double record" in new TestContext {
      runAssert[DoubleWithDefault, Double]("DoubleWithDefault", "double", 5d)
    }

    "error when trying to give a default for a bytes record" in new TestContext {
      runAssert[ByteWithDefault, String]("ByteWithDefault", "bytes", "\"cupcat\"")
    }

  }

  trait TestContext {

    def runAssert[S : AvroSchema, T](typeName: String, valueType: String, default: T) =
      schemaAsString[S] should beRight(simpleSchema(typeName, valueType, default))

    def simpleSchema[T](typeName: String, valueType: String, default: T) = s"""{"type":"record","name":"$typeName","namespace":"unit.SimpleRecordDefaults","doc":"","fields":[{"name":"value","type":"$valueType","doc":"","default":$default}]}"""
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

