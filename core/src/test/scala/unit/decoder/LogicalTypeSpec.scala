package unit.decoder

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.TimeArbitraries._
import unit.utils.UnitSpecBase

class LogicalTypeSpec extends UnitSpecBase {

  "decoder" should {

    "decode UUID" in {
      forAll { writerRecord: WriterRecordWithUUID =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithUUID]
        implicit val codec = Codec[ReaderRecordWithUUID]

        val record = new GenericData.Record(writerSchema.data.value.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.field.toString)
        record.put(2, writerRecord.writerField2)

        val expected = ReaderRecordWithUUID(writerRecord.field)
        record.decode[ReaderRecordWithUUID] should beRight(expected)
      }
    }

    "decode OffsetDateTime" in {
      forAll { writerRecord: WriterRecordWithDateTime =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithDateTime].data.value
        implicit val codec = Codec[ReaderRecordWithDateTime]

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toInstant.toEpochMilli)

        val expected = ReaderRecordWithDateTime(writerRecord.field)
        record.decode[ReaderRecordWithDateTime] should beRight(expected)
      }
    }

    "decode Instant" in {
      forAll { writerRecord: WriterRecordWithInstant =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithInstant].data.value
        implicit val codec = Codec[ReaderRecordWithInstant]

        val record = new GenericData.Record(writerSchema.schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toEpochMilli)

        val expected = ReaderRecordWithInstant(writerRecord.field)
        record.decode[ReaderRecordWithInstant] should beRight(expected)
      }
    }

    "decode with an OffsetDateTime default" in {
      val writerSchema   = AvroSchema.toSchema[WriterRecordWithDateTime].data.value
      implicit val codec = Codec[ReaderRecordWithDateTimeDefault]

      val record = new GenericData.Record(writerSchema.schema)

      record.decode[ReaderRecordWithDateTimeDefault] should beRight(ReaderRecordWithDateTimeDefault())
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
