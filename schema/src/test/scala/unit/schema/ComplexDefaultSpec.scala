package unit.schema

import com.rauchenberg.cupcatAvro.schema.AvroSchema
import common.{UnitSpecBase, schemaAsString}
import ComplexDefault._

class ComplexDefaultSpec extends UnitSpecBase {

  "schema" should {

    "have a default value which is a user-defined type like a case class" in new TestContext {
      runAssert[RecordWithCaseClassDefault, Cupcat]("RecordWithCaseClassDefault","record",Cupcat("cupcat", List("cup", "cat")))
    }

  }

  trait TestContext {

    def runAssert[S : AvroSchema,  T](typeName: String, valueType: String, default: T) =
      schemaAsString[S] should beRight(simpleSchema(typeName, valueType, default))

    def simpleSchema[T](typeName: String, valueType: String, default: T) = s"""{"type":"record","name":"$typeName","namespace":"unit.SimpleRecordDefaults","doc":"","fields":[{"name":"value","type":"$valueType","doc":"","default":$default}]}"""
  }

}

private [this] object ComplexDefault {

  case class Cupcat(x: String, y: List[String])
  case class RecordWithCaseClassDefault(x: String, y: Boolean, f: Cupcat = Cupcat("cupcat", List("cup", "cat")))

}
