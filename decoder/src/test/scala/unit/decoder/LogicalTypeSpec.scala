package unit.decoder

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Parser
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.TimeArbitraries._

class LogicalTypeSpec extends UnitSpecBase {

  "decoder" should {

    "decode UUID" in {
      forAll { writerRecord: WriterRecordWithUUID =>
        val schema       = AvroSchema[WriterRecordWithUUID].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithUUID].schema.value

        val record = new GenericData.Record(schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.field)
        record.put(2, writerRecord.writerField2)

        val expected = ReaderRecordWithUUID(writerRecord.field)
        Parser.decode[ReaderRecordWithUUID](readerSchema, record) should beRight(expected)
      }
    }

    "decode OffsetDateTime" in {
      forAll { writerRecord: WriterRecordWithDateTime =>
        val schema       = AvroSchema[WriterRecordWithDateTime].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithDateTime].schema.value

        val record = new GenericData.Record(schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toInstant.toEpochMilli)

        val expected = ReaderRecordWithDateTime(writerRecord.field)
        Parser.decode[ReaderRecordWithDateTime](readerSchema, record) should beRight(expected)
      }
    }

    "decode Instant" in {
      forAll { writerRecord: WriterRecordWithInstant =>
        val schema       = AvroSchema[WriterRecordWithInstant].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithInstant].schema.value

        val record = new GenericData.Record(schema)
        record.put(0, writerRecord.writerField1)
        record.put(1, writerRecord.writerField2)
        record.put(2, writerRecord.field.toEpochMilli)

        val expected = ReaderRecordWithInstant(writerRecord.field)
        Parser.decode[ReaderRecordWithInstant](readerSchema, record) should beRight(expected)
      }
    }
  }

  case class WriterRecordWithUUID(writerField1: Int, field: UUID, writerField2: String)
  case class ReaderRecordWithUUID(field: UUID)

  case class WriterRecordWithDateTime(writerField1: Int, writerField2: String, field: OffsetDateTime)
  case class ReaderRecordWithDateTime(field: OffsetDateTime)

  case class WriterRecordWithInstant(writerField1: Int, writerField2: String, field: Instant)
  case class ReaderRecordWithInstant(field: Instant)

  case class RecordWithDateTime(field: OffsetDateTime)

}
