package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase
import RunRoundTripAssert._

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in new TestContext {
      forAll { record: TestRecord =>
        val expected = new GenericRecordBuilder(new GenericData.Record(testRecordSchema.data.value.schema))
        expected.set("field", record.field.asJava)

        Encoder.encode[TestRecord](record, testRecordEncoder, testRecordSchema.data) should beRight(
          expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of caseclass" in new TestContext {

      forAll { record: RecordWithListOfCaseClass =>
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value

        val rootRecord = new GenericData.Record(recordWithListOfCaseClassSchema.data.value.schema)

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

        Encoder
          .encode[RecordWithListOfCaseClass](record,
                                             recordWithListOfCaseClassEncoder,
                                             recordWithListOfCaseClassSchema.data) should beRight(
          recordBuilder.build
            .asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of Union" in new TestContext {

      forAll { writerRecord: Option[List[String]] =>
        val builder =
          new GenericRecordBuilder(new GenericData.Record(recordWithOptionalListCaseClassSchema.data.value.schema))

        val r = RecordWithOptionalListCaseClass(writerRecord)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Encoder
          .encode[RecordWithOptionalListCaseClass](r,
                                                   recordWithOptionalListCaseClassEncoder,
                                                   recordWithOptionalListCaseClassSchema.data)
          .map(v => v.get(0).asInstanceOf[java.util.List[Any]].asScala) should beRight(
          builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "encode a record with a nested list" in new TestContext {
      forAll { record: RecordWithListOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(recordWithListOfListSchema.data.value.schema))

        val l = record.field.map(_.asJava).asJava
        builder.set("field", l)

        Encoder
          .encode[RecordWithListOfList](record, recordWithListOfListEncoder, recordWithListOfListSchema.data) should beRight(
          builder.build().asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a more nested list" in new TestContext {
      val writerSchema = AvroSchema.toSchema[RecordWithManyListsOfList]
      forAll { record: RecordWithManyListsOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))

        val l = record.field.map(_.map(_.map(_.map(_.asJava).asJava).asJava).asJava).asJava
        builder.set("field", l)

        val result =
          Encoder.encode[RecordWithManyListsOfList](record, recordWithManyListsOfListEncoder, writerSchema.data)
        val resultAsScalaList = result.map(_.get(0).asInstanceOf[java.util.List[Any]].asScala)
        resultAsScalaList should beRight(builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "encode a record with a list of either" in new TestContext {
      forAll { record: RecordWithListOfEither =>
        val builder = new GenericRecordBuilder(recordWithListOfEitherSchema.data.value.schema)

        val values = record.field.map { value =>
          value match {
            case Left(v)  => v
            case Right(v) => v
          }
        }.asJava

        builder.set("field", values)

        val result = Encoder.encode[RecordWithListOfEither](record,
                                                            recordWithListOfEitherEncoder,
                                                            recordWithListOfEitherSchema.data)

        result should beRight(builder.build().asInstanceOf[GenericRecord])

      }
    }

//    "encode a record with deeply nested unions" in new TestContext {
//      implicit val schema  = AvroSchema.toSchema[RecordWithNestedUnions]
//      val encoder = Encoder[RecordWithNestedUnions]
//
//      forAll { record: RecordWithNestedUnions =>
//        val field = record.field.map(_.flatten.flatten).flatten.asJava
//
//        val recordBuilder = new GenericRecordBuilder(schema.data.value.schema)
//        recordBuilder.set("field", field)
//
//        println("encoded : " + Encoder.encode(record))
//
//        Encoder.encode(record) should beRight(recordBuilder.build().asInstanceOf[GenericRecord])
//      }
//
//    }

    "do a roundtrip encode and decode" in new TestContext {
      runRoundTrip[TestRecord]
      runRoundTrip[RecordWithListOfCaseClass]
      runRoundTrip[RecordWithOptionalListCaseClass]
      runRoundTrip[RecordWithListOfList]
      runRoundTrip[RecordWithManyListsOfList]
    }
  }

  trait TestContext {
    implicit val testRecordEncoder = Encoder[TestRecord]
    implicit val testRecordSchema  = AvroSchema.toSchema[TestRecord]

    implicit val recordWithListOfCaseClassEncoder = Encoder[RecordWithListOfCaseClass]
    implicit val recordWithListOfCaseClassSchema  = AvroSchema.toSchema[RecordWithListOfCaseClass]

    implicit val recordWithOptionalListCaseClassEncoder = Encoder[RecordWithOptionalListCaseClass]
    implicit val recordWithOptionalListCaseClassSchema  = AvroSchema.toSchema[RecordWithOptionalListCaseClass]

    implicit val recordWithListOfListEncoder = Encoder[RecordWithListOfList]
    implicit val recordWithListOfListSchema  = AvroSchema.toSchema[RecordWithListOfList]

    implicit val recordWithManyListsOfListEncoder = Encoder[RecordWithManyListsOfList]
    implicit val recordWithManyListsOfListSchema  = AvroSchema.toSchema[RecordWithManyListsOfList]

    implicit val recordWithListOfEitherEncoder = Encoder[RecordWithListOfEither]
    implicit val recordWithListOfEitherSchema  = AvroSchema.toSchema[RecordWithListOfEither]
  }

  case class TestRecord(field: List[String])
  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)
  case class RecordWithListOfCaseClass(field: List[Nested])
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])
  case class RecordWithListOfList(field: List[List[Int]])
  case class RecordWithManyListsOfList(field: List[List[List[List[List[Int]]]]])
  case class RecordWithListOfEither(field: List[Either[String, Int]])
  case class RecordWithNestedUnions(field: List[Option[Option[Option[Int]]]])

}
