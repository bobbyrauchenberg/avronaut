package unit.schema

import com.rauchenberg.avronaut.schema.AvroSchema
import org.scalacheck.{Arbitrary, Gen}
import unit.utils.UnitSpecBase
import unit.common._

class SimpleRecordDefaultsSpec extends UnitSpecBase {

  implicit val arbString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  "schema" should {

    "have a default for a basic boolean record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[BoolWithDefault]
      runAssert[BoolWithDefault, Boolean]("BoolWithDefault", "boolean", true)
    }

    "have a default for a basic string record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[StringWithDefault]
      runAssert[StringWithDefault, String]("StringWithDefault", "string", "\"cupcat\"")
    }

    "have a default for a basic int record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[IntWithDefault]
      runAssert[IntWithDefault, Int]("IntWithDefault", "int", 5)
    }

    "have a default for a basic long record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[LongWithDefault]
      runAssert[LongWithDefault, Long]("LongWithDefault", "long", 5l)
    }

    "have a default for a basic float record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[FloatWithDefault]
      runAssert[FloatWithDefault, Float]("FloatWithDefault", "float", 5f)
    }

    "have a default for a basic double record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[DoubleWithDefault]
      runAssert[DoubleWithDefault, Double]("DoubleWithDefault", "double", 5d)
    }

    "have a default for a bytes record" in new TestContext {
      implicit val schema = AvroSchema.toSchema[ByteWithDefault]
      runAssert[ByteWithDefault, String]("ByteWithDefault", "bytes", "\"cupcat\"")
    }

  }

  case class MyRecordType(name: String)
  trait TestContext {

    def runAssert[A : AvroSchema, B](typeName: String, valueType: String, default: B) =
      schemaAsString[A] shouldBe simpleSchema(typeName, valueType, default)

    def simpleSchema[A](typeName: String, valueType: String, default: A) =
      s"""{"type":"record","name":"$typeName","namespace":"unit.schema.SimpleRecordDefaultsSpec","doc":"",
         |"fields":[{"name":"value","type":"$valueType","doc":"","default":$default}]}""".stripMargin.replace("\n", "")
  }

  case class BoolWithDefault(value: Boolean = true)
  case class StringWithDefault(value: String = "cupcat")
  case class IntWithDefault(value: Int = 5)
  case class LongWithDefault(value: Long = 5L)
  case class FloatWithDefault(value: Float = 5.0f)
  case class DoubleWithDefault(value: Double = 5D)
  case class ByteWithDefault(value: Array[Byte] = "cupcat".getBytes)
}
