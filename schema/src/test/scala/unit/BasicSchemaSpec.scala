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
      AvroSchema[SimpleNull].schema.map(_.toString) should beRight(simpleSchema("null"))
    }

    "be built for a basic boolean record" in new TestContext {
      AvroSchema[SimpleBool].schema.map(_.toString) should beRight(simpleSchema("boolean"))
    }

    "be built for a basic string record" in new TestContext {
      AvroSchema[SimpleString].schema.map(_.toString) should beRight(simpleSchema("string"))
    }

    "be built for a basic int record" in new TestContext {
      AvroSchema[SimpleInt].schema.map(_.toString) should beRight(simpleSchema("int"))
    }

    "be built for a basic long record" in new TestContext {
      AvroSchema[SimpleLong].schema.map(_.toString) should beRight(simpleSchema("long"))
    }

    "be built for a basic float record" in new TestContext {
      AvroSchema[SimpleFloat].schema.map(_.toString) should beRight(simpleSchema("float"))
    }

    "be built for a basic double record" in new TestContext {
      AvroSchema[SimpleDouble].schema.map(_.toString) should beRight(simpleSchema("double"))
    }

    "be built for a basic bytes record" in new TestContext {
      AvroSchema[SimpleByte].schema.map(_.toString) should beRight(simpleSchema("bytes"))
    }
  }

  trait TestContext {
    def simpleSchema(valueType: String) = s"""{"type":"record","fields":[{"name":"value","type":"$valueType"}]}"""
  }

}

sealed trait SimpleCase
case class SimpleNull(value: Null) extends SimpleCase
case class SimpleBool(value: Boolean) extends SimpleCase
case class SimpleString(value: String) extends SimpleCase
case class SimpleInt(value: Int) extends SimpleCase
case class SimpleLong(value: Long) extends SimpleCase
case class SimpleFloat(value: Float) extends SimpleCase
case class SimpleDouble(value: Double) extends SimpleCase
case class SimpleByte(value: Byte) extends SimpleCase

