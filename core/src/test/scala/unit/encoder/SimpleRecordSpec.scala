package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import com.rauchenberg.avronaut.schema.AvroSchema._
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.encoder.RunRoundTripAssert._
import unit.utils.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in {
      forAll { record: TestRecord =>
        val schema   = AvroSchema.toSchema[TestRecord].value
        val expected = new GenericData.Record(schema.schema)
        expected.put("string", record.string)
        expected.put("boolean", record.boolean)
        expected.put("int", record.int)
        expected.put("float", record.float)
        expected.put("double", record.double)
        expected.put("long", record.long)
        expected.put("bytes", record.bytes)

        Encoder.encode[TestRecord](record, schema) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives to a suitable record" in {
      val writerSchema = AvroSchema.toSchema[NestedRecord].value
      forAll { record: NestedRecord =>
        val expected    = new GenericData.Record(writerSchema.schema)
        val innerRecord = new GenericData.Record(AvroSchema.toSchema[Inner].value.schema)

        innerRecord.put(0, record.inner.value)
        innerRecord.put(1, record.inner.value2)
        expected.put(0, record.string)
        expected.put(1, record.boolean)
        expected.put(2, innerRecord)

        Encoder.encode[NestedRecord](record, writerSchema) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives roundtrip" in {
      runRoundTrip[TestRecord]
      runRoundTrip[NestedRecord]
    }
  }

  case class TestRecord(string: String,
                        boolean: Boolean,
                        int: Int,
                        float: Float,
                        double: Double,
                        long: Long,
                        bytes: Array[Byte])
  case class NestedRecord(string: String, boolean: Boolean, inner: Inner)
  case class Inner(value: String, value2: Int)
}
