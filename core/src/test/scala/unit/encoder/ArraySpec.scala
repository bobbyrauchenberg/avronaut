package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.encoder.RunRoundTripAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in {
      implicit val writerSchema = AvroSchema.toSchema[TestRecord]
      forAll { record: TestRecord =>
        val expected = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))
        expected.set("field", record.field.asJava)

        Encoder.encode[TestRecord](record) should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of caseclass" in {

      implicit val writerSchema = AvroSchema.toSchema[RecordWithListOfCaseClass]

      forAll { record: RecordWithListOfCaseClass =>
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value

        val rootRecord = new GenericData.Record(writerSchema.data.value.schema)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = record.field.zipWithIndex.map {
          case (outer, _) =>
            val outerRecord = new GenericData.Record(outerSchema.schema)
            val innerRecord = new GenericData.Record(innerSchema.schema)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)

        Encoder.encode[RecordWithListOfCaseClass](record) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of Union" in {

      implicit val writerSchema = AvroSchema.toSchema[RecordWithOptionalListCaseClass]

      forAll { writerRecord: Option[List[String]] =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))

        val r = RecordWithOptionalListCaseClass(writerRecord)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Encoder
          .encode[RecordWithOptionalListCaseClass](r)
          .map(v => v.get(0).asInstanceOf[java.util.List[Any]].asScala) should beRight(
          builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "encode a record with a nested list" in {
      implicit val writerSchema = AvroSchema.toSchema[RecordWithListOfList]
      forAll { record: RecordWithListOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))

        val l = record.field.map(_.asJava).asJava
        builder.set("field", l)

        Encoder.encode[RecordWithListOfList](record) should beRight(builder.build())
      }
    }

    "encode a record with a more nested list" in {
      implicit val writerSchema = AvroSchema.toSchema[RecordWithManyListsOfList]
      forAll { record: RecordWithManyListsOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))

        val l = record.field.map(_.map(_.map(_.map(_.asJava).asJava).asJava).asJava).asJava
        builder.set("field", l)

        val result            = Encoder.encode[RecordWithManyListsOfList](record)
        val resultAsScalaList = result.map(_.get(0).asInstanceOf[java.util.List[Any]].asScala)
        resultAsScalaList should beRight(builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "do a roundtrip encode and decode" in {
      implicit val testRecordSchema                = AvroSchema.toSchema[TestRecord]
      implicit val recordWithListOfCaseClassSchema = AvroSchema.toSchema[RecordWithListOfCaseClass]
      implicit val recordWithOptionalListCaseClass = AvroSchema.toSchema[RecordWithOptionalListCaseClass]
      implicit val recordWithListOfList            = AvroSchema.toSchema[RecordWithListOfList]
      implicit val recordWithManyListsOfList       = AvroSchema.toSchema[RecordWithManyListsOfList]

      runRoundTrip[TestRecord]
      runRoundTrip[RecordWithListOfCaseClass]
      runRoundTrip[RecordWithOptionalListCaseClass]
      runRoundTrip[RecordWithListOfList]
      runRoundTrip[RecordWithManyListsOfList]
    }
  }

  case class TestRecord(field: List[String])
  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithListOfCaseClass(field: List[Nested])
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])
  case class RecordWithListOfList(field: List[List[Int]])
  case class RecordWithManyListsOfList(field: List[List[List[List[List[Int]]]]])

}
