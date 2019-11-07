package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.encoder.RunRoundTripAssert._
import unit.utils.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in new TestContext {
      forAll { record: TestRecord =>
        val expected = new GenericData.Record(testRecordSchema.data.value.schema)
        expected.put("string", record.string)
        expected.put("boolean", record.boolean)
        expected.put("int", record.int)
        expected.put("float", record.float)
        expected.put("double", record.double)
        expected.put("long", record.long)
        expected.put("bytes", record.bytes)

        Encoder.encode[TestRecord](
          record,
          testRecordEncoder
        ) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives to a suitable record" in new TestContext {
      forAll { record: NestedRecord =>
        val expected    = new GenericData.Record(nestedRecordSchema.data.value.schema)
        val innerRecord = new GenericData.Record(AvroSchema.toSchema[Inner].data.value.schema)

        innerRecord.put(0, record.inner.value)
        innerRecord.put(1, record.inner.value2)
        expected.put(0, record.string)
        expected.put(1, record.boolean)
        expected.put(2, innerRecord)

        Encoder.encode[NestedRecord](
          record,
          nestedRecordEncoder
        ) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives roundtrip" in new TestContext {
      runRoundTrip[TestRecord]
      runRoundTrip[NestedRecord]
    }
  }

  trait TestContext {
    implicit val testRecordEncoder = Encoder[TestRecord]
    implicit val testRecordDecoder = Decoder[TestRecord]
    implicit val testRecordSchema  = AvroSchema.toSchema[TestRecord]

    implicit val nestedRecordEncoder = Encoder[NestedRecord]
    implicit val nestedRecordDecoder = Decoder[NestedRecord]
    implicit val nestedRecordSchema  = AvroSchema.toSchema[NestedRecord]
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
