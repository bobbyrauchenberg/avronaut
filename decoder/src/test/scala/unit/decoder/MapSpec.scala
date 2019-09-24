package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Parser
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}

import scala.collection.JavaConverters._
class MapSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a map" in {

      forAll { record: WriterRecordWithMap =>
        val writerSchema = AvroSchema[WriterRecordWithMap].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithMap].schema.value

        val genericRecord = new GenericData.Record(writerSchema)

        val recordBuilder = new GenericRecordBuilder(genericRecord)
        recordBuilder.set("writerField", record.writerField)
        recordBuilder.set("field1", record.field1.asJava)
        recordBuilder.set("field2", record.field2)

        val expected = ReaderRecordWithMap(record.field1, record.field2)
        Parser.decode[ReaderRecordWithMap](readerSchema, recordBuilder.build()) should beRight(expected)
      }
    }

    "decode a record with a map of records" in {
      forAll { record: WriterRecordWithMapOfRecord =>
        whenever(record.field2.size > 0) {
          val writerSchema       = AvroSchema[WriterRecordWithMapOfRecord].schema.value
          val writerNestedSchema = AvroSchema[Nested].schema.value

          val readerSchema = AvroSchema[ReaderRecordWithMapOfRecord].schema.value

          val nestedGenericRecord = new GenericData.Record(writerNestedSchema)
          nestedGenericRecord.put("field1", 5)
          nestedGenericRecord.put("field2", "cupcat")

          val nestedMap = record.field2.mapValues { nested =>
            val nestedGenericRecord = new GenericData.Record(writerNestedSchema)
            nestedGenericRecord.put("field1", nested.field1)
            nestedGenericRecord.put("field2", nested.field2)
            nestedGenericRecord
          }.asJava

          val genericRecord = new GenericData.Record(writerSchema)
          val recordBuilder = new GenericRecordBuilder(genericRecord)

          recordBuilder.set("writerField", record.writerField)
          recordBuilder.set("field1", record.field1)
          recordBuilder.set("field2", nestedMap)

          val expected = ReaderRecordWithMapOfRecord(record.field1, record.field2)

          Parser.decode[ReaderRecordWithMapOfRecord](readerSchema, recordBuilder.build()) should beRight(expected)
        }
      }
    }

    "decode a record with a map of Array" in {
      forAll { record: WriterRecordWithList =>
        val writerSchema = AvroSchema[WriterRecordWithList].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithList].schema.value

        val genericRecord = new GenericData.Record(writerSchema)
        val recordBuilder = new GenericRecordBuilder(genericRecord)

        val javaCollection = record.field2.mapValues { list =>
          list.asJava
        }.asJava

        recordBuilder.set("writerField", record.writerField)
        recordBuilder.set("field1", record.field1)
        recordBuilder.set("field2", javaCollection)

        val expected = ReaderRecordWithList(record.field2, record.field1)
        Parser.decode[ReaderRecordWithList](readerSchema, recordBuilder.build()) should beRight(expected)
      }
    }

    "decode a record with a map of Union" in {
      forAll { record: WriterRecordWithUnion =>
        val writerSchema = AvroSchema[WriterRecordWithUnion].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithUnion].schema.value

        val genericRecord = new GenericData.Record(writerSchema)
        val recordBuilder = new GenericRecordBuilder(genericRecord)

        val javaMap = record.field1.mapValues {
          _ match {
            case Left(string)   => string
            case Right(boolean) => boolean
          }
        }.asJava

        recordBuilder.set("field1", javaMap)
        recordBuilder.set("writerField", record.writerField)
        recordBuilder.set("field2", record.field2)

        val expected = ReaderRecordWithUnion(record.field2, record.field1)

        Parser.decode[ReaderRecordWithUnion](readerSchema, recordBuilder.build()) should beRight(expected)
      }
    }
  }

  case class WriterRecordWithMap(writerField: String, field1: Map[String, Boolean], field2: Int)
  case class ReaderRecordWithMap(field1: Map[String, Boolean], field2: Int)

  case class Nested(field1: Int, field2: String)
  case class WriterRecordWithMapOfRecord(field1: Int, writerField: Boolean, field2: Map[String, Nested])
  case class ReaderRecordWithMapOfRecord(field1: Int, field2: Map[String, Nested])

  case class WriterRecordWithList(writerField: Boolean, field1: String, field2: Map[String, List[Int]])
  case class ReaderRecordWithList(field2: Map[String, List[Int]], field1: String)

  case class WriterRecordWithUnion(field1: Map[String, Either[Boolean, Int]], writerField: Boolean, field2: String)
  case class ReaderRecordWithUnion(field2: String, field1: Map[String, Either[Boolean, Int]])
}
