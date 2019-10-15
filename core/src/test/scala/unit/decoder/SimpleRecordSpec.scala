package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.utils.RunAssert._
import unit.utils.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  implicit val dc = Decoder[RecordWithMultipleFields]

  "decoder" should {
    "decode a record with a string field" in {
      forAll { recordWithMultipleFields: RecordWithMultipleFields =>
        val schemaData            = AvroSchema.toSchema[RecordWithMultipleFields]
        val writerSchema          = schemaData.data.value.schema
        implicit val readerSchema = AvroSchema.toSchema[ReaderStringRecord]

        val record = new GenericData.Record(writerSchema)
        record.put(0, recordWithMultipleFields.field1)
        record.put(1, recordWithMultipleFields.field2)
        record.put(2, recordWithMultipleFields.field3)

        Decoder.decode[ReaderStringRecord](record) should beRight(ReaderStringRecord(recordWithMultipleFields.field2))
      }
    }
    "decode a record with a boolean field" in {
      forAll { record: BooleanRecord =>
        implicit val schema = AvroSchema.toSchema[BooleanRecord]
        runDecodeAssert(record.field, record)
      }
    }

    "decode a record with a boolean field with a reader schema" in {
      forAll { recordWithMultipleFields: RecordWithMultipleFields =>
        val writerSchema          = AvroSchema.toSchema[RecordWithMultipleFields].data.value
        implicit val readerSchema = AvroSchema.toSchema[ReaderBooleanRecord]

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, recordWithMultipleFields.field1)
        record.put(1, recordWithMultipleFields.field2)
        record.put(2, recordWithMultipleFields.field3)

        Decoder.decode[ReaderBooleanRecord](record) should beRight(ReaderBooleanRecord(recordWithMultipleFields.field1))
      }
    }

    "decode a record with an int field" in {
      forAll { record: IntRecord =>
        implicit val schema = AvroSchema.toSchema[IntRecord]
        runDecodeAssert(record.field, record)
      }
    }
    "decode a record with a long field" in {
      forAll { record: LongRecord =>
        implicit val schema = AvroSchema.toSchema[LongRecord]
        runDecodeAssert(record.field, record)
      }
    }
    "decode a record with a float field" in {
      forAll { record: FloatRecord =>
        implicit val schema = AvroSchema.toSchema[FloatRecord]
        runDecodeAssert(record.field, record)
      }
    }
    "decode a record with a double field" in {
      forAll { record: DoubleRecord =>
        implicit val schema = AvroSchema.toSchema[DoubleRecord]
        runDecodeAssert(record.field, record)
      }
    }

    "handle simple nested case classes" in {
      forAll { outer: Outer =>
        implicit val schema = AvroSchema.toSchema[Outer]
        val innerSchema     = AvroSchema.toSchema[Inner].data.value

        val outRec = new GenericData.Record(schema.data.value.schema)
        val inRec  = new GenericData.Record(innerSchema.schema)

        inRec.put(0, outer.outerField2.innerField1)
        inRec.put(1, outer.outerField2.innerField2)
        inRec.put(2, outer.outerField2.duplicateFieldName)

        outRec.put(0, outer.outerField1)
        outRec.put(1, inRec)
        outRec.put(2, outer.duplicateFieldName)

        Decoder.decode[Outer](outRec) should beRight(outer)
      }

    }

    "handle defaults" in {
      val writerSchema          = AvroSchema.toSchema[WriterRecordWithDefault].data.value
      implicit val readerSchema = AvroSchema.toSchema[ReaderRecordWithDefault]

      val genericRecord = new GenericData.Record(writerSchema.schema)

      Decoder.decode[ReaderRecordWithDefault](genericRecord) should beRight(ReaderRecordWithDefault())
    }

    "handle nested case class defaults" in {
      forAll { i: Int =>
        val writerSchema          = AvroSchema.toSchema[WriterRecordWithNestedDefault].data.value
        implicit val readerSchema = AvroSchema.toSchema[ReaderRecordWithNestedDefault]

        val genericRecord = new GenericData.Record(writerSchema.schema)
        genericRecord.put("field3", i)

        Decoder.decode[ReaderRecordWithNestedDefault](genericRecord) should beRight(
          ReaderRecordWithNestedDefault(field3 = i))
      }

    }

  }

  case class BooleanRecord(field: Boolean)
  case class ReaderBooleanRecord(field1: Boolean)

  case class RecordWithMultipleFields(field1: Boolean, field2: String, field3: Int)
  case class ReaderStringRecord(field2: String)

  case class IntRecord(field: Int)
  case class LongRecord(field: Long)
  case class FloatRecord(field: Float)
  case class DoubleRecord(field: Double)
  case class StringRecord(field: String)
  case class BytesRecord(field: Array[Byte])
  case class NestedRecord(field: IntRecord)

  case class Inner(innerField1: String, innerField2: Int, duplicateFieldName: String = "rendal")
  case class Outer(outerField1: String, outerField2: Inner, duplicateFieldName: String)

  case class WriterRecordWithDefault(field1: String, field2: Boolean, field3: Int)
  case class ReaderRecordWithDefault(field2: Boolean = true, field3: Int = 123)

  case class WriterRecordWithNestedDefault(field1: String, field2: Outer, field3: Int)
  case class ReaderRecordWithNestedDefault(name: String = "cupcat",
                                           field2: Outer = Outer("cup", Inner("cupInner", 123), "rendal"),
                                           field3: Int)

}
