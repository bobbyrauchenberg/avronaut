package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.utils.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in new TestContext {
      forAll { record: TestRecord =>
        val schema   = Codec.schema[TestRecord].value
        val expected = new GenericData.Record(schema)
        expected.put("string", record.string)
        expected.put("boolean", record.boolean)
        expected.put("int", record.int)
        expected.put("float", record.float)
        expected.put("double", record.double)
        expected.put("long", record.long)

        record.encode should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with nested primitives to a suitable record" in new TestContext {
      forAll { record: NestedRecord =>
        val schema      = Codec.schema[NestedRecord].value
        val expected    = new GenericData.Record(schema)
        val innerRecord = new GenericData.Record(AvroSchema.toSchema[Inner].data.value.schema)

        innerRecord.put(0, record.inner.value)
        innerRecord.put(1, record.inner.value2)
        expected.put(0, record.string)
        expected.put(1, record.boolean)
        expected.put(2, innerRecord)

        record.encode should beRight(expected.asInstanceOf[GenericRecord])
      }
    }
  }

  trait TestContext {
    implicit val testRecordCodec: Codec[TestRecord]     = Codec[TestRecord]
    implicit val nestedRecordCodec: Codec[NestedRecord] = Codec[NestedRecord]
  }

  case class TestRecord(string: String, boolean: Boolean, int: Int, float: Float, double: Double, long: Long)
  case class NestedRecord(string: String, boolean: Boolean, inner: Inner)
  case class Inner(value: String, value2: Int)
}
