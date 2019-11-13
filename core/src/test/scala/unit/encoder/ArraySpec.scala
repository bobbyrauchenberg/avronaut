package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in new TestContext {
      forAll { record: TestRecord =>
        val schema   = Codec.schema[TestRecord].value
        val expected = new GenericRecordBuilder(new GenericData.Record(schema))
        expected.set("field", record.field.asJava)

        record.encode should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of caseclass" in new TestContext {

      forAll { record: RecordWithListOfCaseClass =>
        val schema      = Codec.schema[RecordWithListOfCaseClass].value
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value

        val rootRecord = new GenericData.Record(schema)

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

        record.encode should beRight(
          recordBuilder.build
            .asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of Union" in new TestContext {

      forAll { writerRecord: Option[List[String]] =>
        val schema = Codec.schema[RecordWithOptionalListCaseClass].value
        val builder =
          new GenericRecordBuilder(new GenericData.Record(schema))

        val r = RecordWithOptionalListCaseClass(writerRecord)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        r.encode should beRight(builder.build().asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a nested list" in new TestContext {
      forAll { record: RecordWithListOfList =>
        val schema  = Codec.schema[RecordWithListOfList].value
        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        val l = record.field.map(_.asJava).asJava
        builder.set("field", l)

        record.encode should beRight(builder.build().asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a more nested list" in new TestContext {
      val writerSchema = AvroSchema.toSchema[RecordWithManyListsOfList]
      forAll { record: RecordWithManyListsOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.data.value.schema))

        val l = record.field.map(_.map(_.map(_.map(_.asJava).asJava).asJava).asJava).asJava
        builder.set("field", l)

        val result            = record.encode
        val resultAsScalaList = result.map(_.get(0).asInstanceOf[java.util.List[Any]].asScala)
        resultAsScalaList should beRight(builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "encode a record with a list of either" in new TestContext {
      forAll { record: RecordWithListOfEither =>
        val schema  = Codec.schema[RecordWithListOfEither].value
        val builder = new GenericRecordBuilder(schema)

        val values = record.field.map { value =>
          value match {
            case Left(v)  => v
            case Right(v) => v
          }
        }.asJava

        builder.set("field", values)

        record.encode should beRight(builder.build().asInstanceOf[GenericRecord])
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

  }

  trait TestContext {
    implicit val testRecordCodec: Codec[TestRecord]                               = Codec[TestRecord]
    implicit val recordWithListOfCaseClassCodec: Codec[RecordWithListOfCaseClass] = Codec[RecordWithListOfCaseClass]
    implicit val recordWithOptionalListCaseClassCodec: Codec[RecordWithOptionalListCaseClass] =
      Codec[RecordWithOptionalListCaseClass]
    implicit val recordWithListOfListCodec: Codec[RecordWithListOfList]           = Codec[RecordWithListOfList]
    implicit val recordWithManyListsOfListCodec: Codec[RecordWithManyListsOfList] = Codec[RecordWithManyListsOfList]
    implicit val recordWithListOfEitherCodec: Codec[RecordWithListOfEither]       = Codec[RecordWithListOfEither]
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
