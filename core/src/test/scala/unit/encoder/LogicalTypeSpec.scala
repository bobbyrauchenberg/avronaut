package unit.encoder

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.common.TimeArbitraries._
import unit.utils.UnitSpecBase

class LogicalTypeSpec extends UnitSpecBase {

  "encoder" should {

    "encode UUID" in {
      forAll { record: RecordWithUUID =>
        val schema         = AvroSchema.toSchema[RecordWithUUID]
        implicit val codec = Codec[RecordWithUUID]

        val genericRecord = new GenericData.Record(schema.data.value.schema)
        genericRecord.put(0, record.writerField1)
        genericRecord.put(1, record.field.toString)
        genericRecord.put(2, record.writerField2)

        record.encode should beRight(genericRecord.asInstanceOf[GenericRecord])
      }
    }

    "encode OffsetDateTime" in {
      forAll { record: RecordWithDateTime =>
        val schema         = AvroSchema.toSchema[RecordWithDateTime]
        implicit val codec = Codec[RecordWithDateTime]

        val genericRecord = new GenericData.Record(schema.data.value.schema)
        genericRecord.put(0, record.writerField1)
        genericRecord.put(1, record.writerField2)
        genericRecord.put(2, record.field.toInstant.toEpochMilli)

        record.encode should beRight(genericRecord.asInstanceOf[GenericRecord])
      }
    }

    "encode Instant" in {
      forAll { record: RecordWithInstant =>
        val schema         = AvroSchema.toSchema[RecordWithInstant]
        implicit val codec = Codec[RecordWithInstant]

        val genericRecord = new GenericData.Record(schema.data.value.schema)
        genericRecord.put(0, record.writerField1)
        genericRecord.put(1, record.writerField2)
        genericRecord.put(2, record.field.toEpochMilli)

        record.encode should beRight(genericRecord.asInstanceOf[GenericRecord])
      }
    }

    "encode with an OffsetDateTime default" in {
      val schema              = AvroSchema.toSchema[RecordWithDateTimeDefault]
      implicit val codec      = Codec[RecordWithDateTimeDefault]
      val defaultDateTimeLong = defaultDateTime.toInstant.toEpochMilli

      val genericRecord = new GenericData.Record(schema.data.value.schema)
      genericRecord.put(0, defaultDateTimeLong)

      RecordWithDateTimeDefault().encode should beRight(genericRecord.asInstanceOf[GenericRecord])
    }
  }

  case class RecordWithUUID(writerField1: Int, field: UUID, writerField2: String)
  case class ReaderRecordWithUUID(field: UUID)

  case class RecordWithDateTime(writerField1: Int, writerField2: String, field: OffsetDateTime)
  case class ReaderRecordWithDateTime(field: OffsetDateTime)

  case class RecordWithInstant(writerField1: Int, writerField2: String, field: Instant)
  case class ReaderRecordWithInstant(field: Instant)

  val defaultDateTime = OffsetDateTime.of(LocalDateTime.of(2017, 9, 1, 1, 0), ZoneOffset.ofHoursMinutes(6, 30))
  case class RecordWithDateTimeDefault(field: OffsetDateTime = defaultDateTime)

}
