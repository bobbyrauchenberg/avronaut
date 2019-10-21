package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class MapSpec extends UnitSpecBase {

  "encode a record with a map" in new TestContext {
    forAll { writerRecord: WriterRecordWithMap =>
      val schema = Codec.schema[WriterRecordWithMap].value
      val record = new GenericData.Record(schema)

      val recordBuilder = new GenericRecordBuilder(record)
      recordBuilder.set("writerField", writerRecord.writerField)
      recordBuilder.set("field1", writerRecord.field1.asJava)
      recordBuilder.set("field2", writerRecord.field2)

      val expected = recordBuilder.build()

      writerRecord.encode should beRight(expected.asInstanceOf[GenericRecord])

    }
  }

  "encode a record with a map of records" in new TestContext {
    forAll { writerRecord: WriterRecordWithMapOfRecord =>
      val schema = Codec.schema[WriterRecordWithMapOfRecord].value

      whenever(writerRecord.field2.size > 0) {
        val nestedSchema = AvroSchema.toSchema[Nested].data.value

        val nestedGenericRecord = new GenericData.Record(nestedSchema.schema)
        nestedGenericRecord.put("field1", 5)
        nestedGenericRecord.put("field2", "cupcat")

        val nestedMap = writerRecord.field2.mapValues { nested =>
          val nestedGenericRecord = new GenericData.Record(nestedSchema.schema)
          nestedGenericRecord.put("field1", nested.field1)
          nestedGenericRecord.put("field2", nested.field2)
          nestedGenericRecord
        }.asJava

        val genericRecord = new GenericData.Record(schema)
        val recordBuilder = new GenericRecordBuilder(genericRecord)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", writerRecord.field1)
        recordBuilder.set("field2", nestedMap)

        writerRecord.encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }
  }

  "encode a record with a map of Array" in new TestContext {
    forAll { writerRecord: WriterRecordWithList =>
      val schema        = Codec.schema[WriterRecordWithList].value
      val record        = new GenericData.Record(schema)
      val recordBuilder = new GenericRecordBuilder(record)

      val javaCollection = writerRecord.field2.mapValues { list =>
        list.asJava
      }.asJava

      recordBuilder.set("writerField", writerRecord.writerField)
      recordBuilder.set("field1", writerRecord.field1)
      recordBuilder.set("field2", javaCollection)

      writerRecord.encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
    }
  }

  case class WriterRecordWithMap(writerField: String, field1: Map[String, Boolean], field2: Int)
  case class ReaderRecordWithMap(field1: Map[String, Boolean], field2: Int)

  case class Nested(field1: Int, field2: String)
  case class WriterRecordWithMapOfRecord(field1: Int, writerField: Boolean, field2: Map[String, Nested])

  case class WriterRecordWithList(writerField: Boolean, field1: String, field2: Map[String, List[Int]])

  trait TestContext {
    implicit val writeRecordWithMapCodec: Codec[WriterRecordWithMap] = Codec[WriterRecordWithMap]
    implicit val writerRecordWithMapOfRecordCodec: Codec[WriterRecordWithMapOfRecord] =
      Codec[WriterRecordWithMapOfRecord]
    implicit val writerRecordWithListCodec: Codec[WriterRecordWithList] = Codec[WriterRecordWithList]
  }
}
