package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.scalacheck.Arbitrary
import unit.common.RunRoundTripAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class MapSpec extends UnitSpecBase {

  "encode a record with a map" in new TestContext {
    forAll { writerRecord: WriterRecordWithMap =>
      val record = new GenericData.Record(writerRecordWithMapSchema.data.value.schema)

      val recordBuilder = new GenericRecordBuilder(record)
      recordBuilder.set("writerField", writerRecord.writerField)
      recordBuilder.set("field1", writerRecord.field1.asJava)
      recordBuilder.set("field2", writerRecord.field2)

      val expected = recordBuilder.build()

      Encoder.encode(writerRecord, writeRecordWithMapEncoder) should beRight(expected.asInstanceOf[GenericRecord])

    }
  }

  "encode a record with a map of records" in new TestContext {
    forAll { writerRecord: WriterRecordWithMapOfRecord =>
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

        val genericRecord = new GenericData.Record(writerRecordWithMapOfRecordSchema.data.value.schema)
        val recordBuilder = new GenericRecordBuilder(genericRecord)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", writerRecord.field1)
        recordBuilder.set("field2", nestedMap)

        Encoder.encode(writerRecord, writerRecordWithMapOfRecordEncoder) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }
  }

  "encode a record with a map of Array" in new TestContext {
    forAll { writerRecord: WriterRecordWithList =>
      val record        = new GenericData.Record(writerRecordWithListSchema.data.value.schema)
      val recordBuilder = new GenericRecordBuilder(record)

      val javaCollection = writerRecord.field2.mapValues { list =>
        list.asJava
      }.asJava

      recordBuilder.set("writerField", writerRecord.writerField)
      recordBuilder.set("field1", writerRecord.field1)
      recordBuilder.set("field2", javaCollection)

      Encoder.encode(writerRecord, writerRecordWithListEncoder) should beRight(
        recordBuilder.build.asInstanceOf[GenericRecord])
    }
  }

  "do a roundtrip encode and decode" in new TestContext {
    implicitly[Arbitrary[WriterRecordWithMapOfRecord]]
    runRoundTrip[WriterRecordWithMapOfRecord](
      implicitly[Arbitrary[WriterRecordWithMapOfRecord]],
      writerRecordWithMapOfRecordEncoder,
      writerRecordWithMapOfRecordDecoder
    )
    runRoundTrip[WriterRecordWithMap](implicitly[Arbitrary[WriterRecordWithMap]],
                                      writeRecordWithMapEncoder,
                                      writerRecordWithMapDecoder)
    runRoundTrip[WriterRecordWithList](implicitly[Arbitrary[WriterRecordWithList]],
                                       writerRecordWithListEncoder,
                                       writerRecordWithListDecoder)
  }

  case class WriterRecordWithMap(writerField: String, field1: Map[String, Boolean], field2: Int)
  case class ReaderRecordWithMap(field1: Map[String, Boolean], field2: Int)

  case class Nested(field1: Int, field2: String)
  case class WriterRecordWithMapOfRecord(field1: Int, writerField: Boolean, field2: Map[String, Nested])

  case class WriterRecordWithList(writerField: Boolean, field1: String, field2: Map[String, List[Int]])

  trait TestContext {
    implicit val writeRecordWithMapEncoder  = Encoder[WriterRecordWithMap]
    implicit val writerRecordWithMapSchema  = AvroSchema.toSchema[WriterRecordWithMap]
    implicit val writerRecordWithMapDecoder = Decoder[WriterRecordWithMap]

    implicit val writerRecordWithMapOfRecordEncoder =
      Encoder[WriterRecordWithMapOfRecord]
    implicit val writerRecordWithMapOfRecordSchema =
      AvroSchema.toSchema[WriterRecordWithMapOfRecord]
    implicit val writerRecordWithMapOfRecordDecoder = Decoder[WriterRecordWithMapOfRecord]

    implicit val writerRecordWithListEncoder = Encoder[WriterRecordWithList]
    implicit val writerRecordWithListSchema =
      AvroSchema.toSchema[WriterRecordWithList]
    implicit val writerRecordWithListDecoder = Decoder[WriterRecordWithList]
  }
}
