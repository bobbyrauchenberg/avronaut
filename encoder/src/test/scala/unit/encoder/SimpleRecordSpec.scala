package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.common.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in {
      forAll { record: TestRecord =>
        val schema   = AvroSchema[TestRecord].schema.value
        val expected = new GenericData.Record(schema)
        expected.put("string", record.string)
        expected.put("boolean", record.boolean)
        expected.put("int", record.int)

        Encoder.encode[TestRecord](record) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with supported primitives roundtrip" in {
      forAll { record: TestRecord =>
        Encoder.encode[TestRecord](record).flatMap { v =>
          Decoder.decode[TestRecord](v)
        } should beRight(record)
      }
    }

    "encode a case class with nested primitives to a suitable record" in {
      forAll { record: NestedRecord =>
        val schema      = AvroSchema[NestedRecord].schema.value
        val expected    = new GenericData.Record(schema)
        val innerRecord = new GenericData.Record(AvroSchema[Inner].schema.value)

        innerRecord.put(0, record.inner.value)
        innerRecord.put(1, record.inner.value2)
        expected.put(0, record.string)
        expected.put(1, record.boolean)
        expected.put(2, innerRecord)


        Encoder.encode[NestedRecord](record) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives roundtrip" in {
      forAll { record: NestedRecord =>
        Encoder.encode[NestedRecord](record).flatMap { v =>
          Decoder.decode[NestedRecord](v)
        } should beRight(record)
      }
    }

  }

  case class TestRecord(string: String, boolean: Boolean, int: Int)
  case class NestedRecord(string: String, boolean: Boolean, inner: Inner)
  case class Inner(value: String, value2: Int)
}
