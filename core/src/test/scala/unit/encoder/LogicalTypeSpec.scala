//package unit.encoder
//
//import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
//import java.util.UUID
//
//import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
//import com.rauchenberg.avronaut.encoder.Encoder
//import com.rauchenberg.avronaut.schema.AvroSchema
//import org.apache.avro.generic.GenericData
//import unit.utils.UnitSpecBase
//import unit.common.TimeArbitraries._
//
//class LogicalTypeSpec extends UnitSpecBase {
//
//  "encoder" should {
//
//    "encode UUID" in {
//      forAll { record: RecordWithUUID =>
//        implicit val schema  = AvroSchema.toSchema[RecordWithUUID]
//        implicit val encoder = Encoder[RecordWithUUID]
//
//        val genericRecord = new GenericData.Record(schema.data.value.schema)
//        genericRecord.put(0, record.writerField1)
//        genericRecord.put(1, record.field.toString)
//        genericRecord.put(2, record.writerField2)
//
//        Encoder.encode[RecordWithUUID](record) should beRight(genericRecord)
//      }
//    }
//
//    "encode OffsetDateTime" in {
//      forAll { record: RecordWithDateTime =>
//        implicit val schema  = AvroSchema.toSchema[RecordWithDateTime]
//        implicit val encoder = Encoder[RecordWithDateTime]
//
//        val genericRecord = new GenericData.Record(schema.data.value.schema)
//        genericRecord.put(0, record.writerField1)
//        genericRecord.put(1, record.writerField2)
//        genericRecord.put(2, record.field.toInstant.toEpochMilli)
//
//        Encoder.encode[RecordWithDateTime](record) should beRight(genericRecord)
//      }
//    }
//
//    "encode Instant" in {
//      forAll { record: RecordWithInstant =>
//        implicit val schema  = AvroSchema.toSchema[RecordWithInstant]
//        implicit val encoder = Encoder[RecordWithInstant]
//
//        val genericRecord = new GenericData.Record(schema.data.value.schema)
//        genericRecord.put(0, record.writerField1)
//        genericRecord.put(1, record.writerField2)
//        genericRecord.put(2, record.field.toEpochMilli)
//
//        Encoder.encode[RecordWithInstant](record)
//      }
//    }
//
//    "encode with an OffsetDateTime default" in {
//      implicit val schema     = AvroSchema.toSchema[RecordWithDateTimeDefault]
//      val defaultDateTimeLong = defaultDateTime.toInstant.toEpochMilli
//
//      val genericRecord = new GenericData.Record(schema.data.value.schema)
//      genericRecord.put(0, defaultDateTimeLong)
//
//      Encoder.encode(RecordWithDateTimeDefault()) should beRight(genericRecord)
//    }
//  }
//
//  case class RecordWithUUID(writerField1: Int, field: UUID, writerField2: String)
//  case class ReaderRecordWithUUID(field: UUID)
//
//  case class RecordWithDateTime(writerField1: Int, writerField2: String, field: OffsetDateTime)
//  case class ReaderRecordWithDateTime(field: OffsetDateTime)
//
//  case class RecordWithInstant(writerField1: Int, writerField2: String, field: Instant)
//  case class ReaderRecordWithInstant(field: Instant)
//
//  val defaultDateTime = OffsetDateTime.of(LocalDateTime.of(2017, 9, 1, 1, 0), ZoneOffset.ofHoursMinutes(6, 30))
//  case class RecordWithDateTimeDefault(field: OffsetDateTime = defaultDateTime)
//
//}
