package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._
class MapSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a map" in {

      forAll { writerRecord: WriterRecordWithMap =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithMap]
        implicit val codec = Codec[ReaderRecordWithMap]
        val record         = new GenericData.Record(writerSchema.data.value.schema)

        val recordBuilder = new GenericRecordBuilder(record)
        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", writerRecord.field1.asJava)
        recordBuilder.set("field2", writerRecord.field2)

        val expected = ReaderRecordWithMap(writerRecord.field1, writerRecord.field2)
        recordBuilder.build.decode[ReaderRecordWithMap] should beRight(expected)
      }
    }

    "decode a record with a map of records" in {
      forAll { writerRecord: WriterRecordWithMapOfRecord =>
        whenever(writerRecord.field2.size > 0) {
          val writerSchema   = AvroSchema.toSchema[WriterRecordWithMapOfRecord]
          implicit val codec = Codec[ReaderRecordWithMapOfRecord]

          val writerNestedSchema = AvroSchema.toSchema[Nested]

          val nestedGenericRecord = new GenericData.Record(writerNestedSchema.data.value.schema)
          nestedGenericRecord.put("field1", 5)
          nestedGenericRecord.put("field2", "cupcat")

          val nestedMap = writerRecord.field2.mapValues { nested =>
            val nestedGenericRecord = new GenericData.Record(writerNestedSchema.data.value.schema)
            nestedGenericRecord.put("field1", nested.field1)
            nestedGenericRecord.put("field2", nested.field2)
            nestedGenericRecord
          }.asJava

          val genericRecord = new GenericData.Record(writerSchema.data.value.schema)
          val recordBuilder = new GenericRecordBuilder(genericRecord)

          recordBuilder.set("writerField", writerRecord.writerField)
          recordBuilder.set("field1", writerRecord.field1)
          recordBuilder.set("field2", nestedMap)

          val expected = ReaderRecordWithMapOfRecord(writerRecord.field1, writerRecord.field2)

          recordBuilder.build.decode[ReaderRecordWithMapOfRecord] should beRight(expected)
        }
      }
    }

    "decode a record with a map of Array" in {
      forAll { writerRecord: WriterRecordWithList =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithList]
        implicit val codec = Codec[ReaderRecordWithList]

        val record        = new GenericData.Record(writerSchema.data.value.schema)
        val recordBuilder = new GenericRecordBuilder(record)

        val javaCollection = writerRecord.field2.mapValues { list =>
          list.asJava
        }.asJava

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", writerRecord.field1)
        recordBuilder.set("field2", javaCollection)

        val expected = ReaderRecordWithList(writerRecord.field2, writerRecord.field1)
        recordBuilder.build.decode[ReaderRecordWithList] should beRight(expected)
      }
    }

    "decode a record with a map of Union" in {
      forAll { writerRecord: WriterRecordWithUnion =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithUnion]
        implicit val codec = Codec[ReaderRecordWithUnion]

        val record        = new GenericData.Record(writerSchema.data.value.schema)
        val recordBuilder = new GenericRecordBuilder(record)

        val javaMap = writerRecord.field1.mapValues {
          _ match {
            case Left(string)   => string
            case Right(boolean) => boolean
          }
        }.asJava

        recordBuilder.set("field1", javaMap)
        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field2", writerRecord.field2)

        val expected = ReaderRecordWithUnion(writerRecord.field2, writerRecord.field1)

        recordBuilder.build.decode[ReaderRecordWithUnion] should beRight(expected)
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
