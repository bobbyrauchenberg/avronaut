package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder

import collection.JavaConverters._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.common.UnitSpecBase

class ArrayEncoderSpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in {
      forAll { record: TestRecord =>
        val schema   = AvroSchema[TestRecord].schema.value
        val expected = new GenericRecordBuilder(new GenericData.Record(schema))
        expected.set("list", record.list.asJava)

        Encoder.encode[TestRecord](record) should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of primitives roundtrip" in {
      forAll { record: TestRecord =>
        Encoder.encode[TestRecord](record).flatMap { v =>
          Decoder.decode[TestRecord](v)
        } should beRight(record)
      }
    }

    "encode a record with a list of caseclass" in {

      forAll { record: RecordWithListOfCaseClass =>
        val rootSchema  = AvroSchema[RecordWithListOfCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = record.field.zipWithIndex.map {
          case (outer, _) =>
            val outerRecord = new GenericData.Record(outerSchema.value)
            val innerRecord = new GenericData.Record(innerSchema.value)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)

        Encoder.encode[RecordWithListOfCaseClass](record) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of caseclass roundtrip" in {

      forAll { record: RecordWithListOfCaseClass =>
        Encoder.encode[RecordWithListOfCaseClass](record).flatMap { v =>
          Decoder.decode[RecordWithListOfCaseClass](v)
        } should beRight(record)
      }
    }

  }

  case class TestRecord(list: List[String])

  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithListOfCaseClass(field: List[Nested])
}
