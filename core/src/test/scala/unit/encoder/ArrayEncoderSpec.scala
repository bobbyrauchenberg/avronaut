package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class ArrayEncoderSpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in {
      val writerSchema = AvroSchema.toSchema[TestRecord].value
      forAll { record: TestRecord =>
        val expected = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))
        expected.set("field", record.field.asJava)

        Encoder.encode[TestRecord](record, writerSchema) should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of primitives roundtrip" in {
      val writerSchema = AvroSchema.toSchema[TestRecord].value
      forAll { record: TestRecord =>
        Encoder.encode[TestRecord](record, writerSchema).flatMap { v =>
          Decoder.decode[TestRecord](v, writerSchema)
        } should beRight(record)
      }
    }

    "encode a record with a list of caseclass" in {

      val writerSchema = AvroSchema.toSchema[RecordWithListOfCaseClass].value

      forAll { record: RecordWithListOfCaseClass =>
        val outerSchema = AvroSchema.toSchema[Nested].value
        val innerSchema = AvroSchema.toSchema[InnerNested].value

        val rootRecord = new GenericData.Record(writerSchema.schema)

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

        Encoder.encode[RecordWithListOfCaseClass](record, writerSchema) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of caseclass roundtrip" in {

      val writerSchema = AvroSchema.toSchema[RecordWithListOfCaseClass].value

      forAll { record: RecordWithListOfCaseClass =>
        Encoder.encode[RecordWithListOfCaseClass](record, writerSchema).flatMap { v =>
          Decoder.decode[RecordWithListOfCaseClass](v, writerSchema)
        } should beRight(record)
      }
    }

    "encode a record with a list of Union" in {

      val writerSchema = AvroSchema.toSchema[RecordWithOptionalListCaseClass].value

      forAll { writerRecord: Option[List[String]] =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        val r = RecordWithOptionalListCaseClass(writerRecord)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Encoder
          .encode[RecordWithOptionalListCaseClass](r, writerSchema)
          .map(v => v.get(0).asInstanceOf[java.util.List[Any]].asScala) should beRight(
          builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }
    }

    "encode a record with a list of Union roundtrip" in {
      val writerSchema = AvroSchema.toSchema[RecordWithOptionalListCaseClass].value
      forAll { record: Option[List[String]] =>
        val r = RecordWithOptionalListCaseClass(record)
        Encoder.encode[RecordWithOptionalListCaseClass](r, writerSchema).flatMap { v =>
          Decoder.decode[RecordWithOptionalListCaseClass](v, writerSchema)
        } should beRight(r)
      }
    }

    "encode a record with a nested list" in {
      val writerSchema = AvroSchema.toSchema[RecordWithListOfList].value
      forAll { record: RecordWithListOfList =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        val l = record.field.map(_.asJava).asJava
        builder.set("field", l)

        Encoder.encode[RecordWithListOfList](record, writerSchema) should beRight(builder.build())
      }

    }

    "encode a record with a more nested list" in {
      val writerSchema = AvroSchema.toSchema[RecordWithManyListsOfList].value
      forAll { list: List[Int] =>
        val record = RecordWithManyListsOfList(List(List(List(List(list)))))

        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        val l = record.field.map(_.map(_.map(_.map(_.asJava).asJava).asJava).asJava).asJava
        builder.set("field", l)

        val result            = Encoder.encode[RecordWithManyListsOfList](record, writerSchema)
        val resultAsScalaList = result.map(_.get(0).asInstanceOf[java.util.List[Any]].asScala)
        resultAsScalaList should beRight(builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }

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
