package unit.decoder

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.TimeArbitraries._
import unit.utils.UnitSpecBase

class LogicalTypeSpec extends UnitSpecBase {

  "decoder" should {

    "decode UUID" in {
      forAll { writerRecord: WriterRecordWithUUID =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithUUID].value
        val readerSchema = AvroSchema.toSchema[ReaderRecordWithUUID].value

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.field.toString)
        record.put(2, writerRecord.writerField2)

        val expected = ReaderRecordWithUUID(writerRecord.field)
        Decoder.decode[ReaderRecordWithUUID](record, readerSchema) should beRight(expected)
      }
    }

    "decode OffsetDateTime" in {
      forAll { writerRecord: WriterRecordWithDateTime =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithDateTime].value
        val readerSchema = AvroSchema.toSchema[ReaderRecordWithDateTime].value

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toInstant.toEpochMilli)

        val expected = ReaderRecordWithDateTime(writerRecord.field)
        Decoder.decode[ReaderRecordWithDateTime](record, readerSchema) should beRight(expected)
      }
    }

    "decode Instant" in {
      forAll { writerRecord: WriterRecordWithInstant =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithInstant].value
        val readerSchema = AvroSchema.toSchema[ReaderRecordWithInstant].value

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toEpochMilli)

        val expected = ReaderRecordWithInstant(writerRecord.field)
        Decoder.decode[ReaderRecordWithInstant](record, readerSchema) should beRight(expected)
      }
    }

    "decode with an OffsetDateTime default" in {
      val writerSchema = AvroSchema.toSchema[WriterRecordWithDateTime].value
      val readerSchema = AvroSchema.toSchema[ReaderRecordWithDateTimeDefault].value

      val record = new GenericData.Record(writerSchema.schema)

      Decoder.decode[ReaderRecordWithDateTimeDefault](record, readerSchema) should beRight(
        ReaderRecordWithDateTimeDefault())
    }
  }

  case class WriterRecordWithUUID(writerField1: Int, field: UUID, writerField2: String)
  case class ReaderRecordWithUUID(field: UUID)

  case class WriterRecordWithDateTime(writerField1: Int, writerField2: String, field: OffsetDateTime)
  case class ReaderRecordWithDateTime(field: OffsetDateTime)

  case class WriterRecordWithInstant(writerField1: Int, writerField2: String, field: Instant)
  case class ReaderRecordWithInstant(field: Instant)

  val defaultDateTime = OffsetDateTime.of(LocalDateTime.of(2017, 9, 1, 1, 0), ZoneOffset.ofHoursMinutes(6, 30))
  case class ReaderRecordWithDateTimeDefault(field: OffsetDateTime = defaultDateTime)

}
