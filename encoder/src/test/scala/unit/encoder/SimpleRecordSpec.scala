package unit.encoder

import cats.syntax.either._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.common.{AggregatedError, Error}
import com.rauchenberg.cupcatAvro.encoder.Encoder
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in {
      forAll { data: TestRecord =>
        val schema = AvroSchema[TestRecord].schema.value
        val expectedRecord = new GenericData.Record(schema)
        expectedRecord.put("string", data.string)
        expectedRecord.put("boolean", data.boolean)
        expectedRecord.put("int", data.int)
        expectedRecord.put("long", data.long)
        expectedRecord.put("float", data.float)
        expectedRecord.put("double", data.double)
        expectedRecord.put("bytes", data.bytes)

        Encoder[TestRecord].encode(data, schema) shouldBe expectedRecord.asRight
      }
    }

    "encode a case class with nested primitives to a suitable record" in {
      forAll { data: NestedRecord =>
        val schema = AvroSchema[NestedRecord].schema.value
        val expectedRecord = new GenericData.Record(schema)
        val innerRecord = new GenericData.Record(AvroSchema[Inner].schema.value)
        innerRecord.put("value", data.inner.value)
        expectedRecord.put("string", data.string)
        expectedRecord.put("boolean", data.boolean)
        expectedRecord.put("inner", innerRecord)

        Encoder[NestedRecord].encode(data, schema) shouldBe expectedRecord.asRight
      }
    }

    "return an error when an invalid schema is given" in {
      forAll { data: NestedRecord =>
        case class Broken(somethingElse: String)
        val badSchema = AvroSchema[Broken].schema.value

        val expectedErrorMsg = s"Invalid schema: $badSchema, for value: $data"
        Encoder[NestedRecord].encode(data, badSchema) shouldBe AggregatedError(List(Error(expectedErrorMsg))).asLeft
      }
    }

    "return an error with an incorrectly typed field" in {
      forAll { data: TestRecord =>
        case class Broken(string: Long)
        val badSchema = AvroSchema[Broken].schema.value

        val outerErrorMsg = s"Invalid schema: $badSchema, for value: $data"
        val innerErrorMsg = s"""Invalid schema: "long", for value: ${data.string}"""
        Encoder[TestRecord].encode(data, badSchema) shouldBe
          AggregatedError(List(Error(outerErrorMsg), Error(innerErrorMsg))).asLeft
      }
    }

    "collect all encoding errors from using an invalid schema" in {
      forAll { data: NestedRecord =>
        case class Broken(string: Long, boolean: Int, inner: TestRecord)

        Encoder[NestedRecord].encode(data, AvroSchema[Broken].schema.value).leftMap { error =>
          error shouldBe a [AggregatedError]
          error.asInstanceOf[AggregatedError].errors.length shouldBe 4
        }
      }
    }
  }

  case class TestRecord(string: String, boolean: Boolean, int: Int, long: Long, float: Float, double: Double, bytes: Array[Byte])
  case class NestedRecord(string: String, boolean: Boolean, inner: Inner)
  case class Inner(value: String)
}
