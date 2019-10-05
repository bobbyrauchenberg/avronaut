package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericContainer, GenericData, GenericRecord, GenericRecordBuilder}
import unit.common.UnitSpecBase

import scala.collection.JavaConverters._

class ArrayEncoderSpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in {
      forAll { record: TestRecord =>
        val schema   = AvroSchema[TestRecord].schema.value
        val expected = new GenericRecordBuilder(new GenericData.Record(schema))
        expected.set("field", record.field.asJava)

        Encoder.encode[TestRecord](record) should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a record with a list of primitives roundtrip" in {
      forAll { record: TestRecord =>
        Encoder.encode[TestRecord](record).flatMap { v =>
          Decoder.decode[TestRecord](v)
        } should beRight(record)
      }
    }

    "encode a record with a list of caseclass" in {

      forAll { record: RecordWithListOfCaseClass =>
        val rootSchema  = AvroSchema[RecordWithListOfCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = record.field.zipWithIndex.map {
          case (outer, _) =>
            val outerRecord = new GenericData.Record(outerSchema.value)
            val innerRecord = new GenericData.Record(innerSchema.value)
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

    "encode a record with a list of caseclass roundtrip" in {

      forAll { record: RecordWithListOfCaseClass =>
        Encoder.encode[RecordWithListOfCaseClass](record).flatMap { v =>
          Decoder.decode[RecordWithListOfCaseClass](v)
        } should beRight(record)
      }
    }

    "encode a record with a list of Union" in {
      forAll { writerRecord: Option[List[String]] =>
        val schema = AvroSchema[RecordWithOptionalListCaseClass].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

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

    "encode a record with a list of Union roundtrip" in {
      forAll { record: Option[List[String]] =>
        val r = RecordWithOptionalListCaseClass(record)
        Encoder.encode[RecordWithOptionalListCaseClass](r).flatMap { v =>
          Decoder.decode[RecordWithOptionalListCaseClass](v)
        } should beRight(r)
      }
    }

    "encode a record with a nested list" in {
      forAll { record: RecordWithListOfList =>
        val schema = AvroSchema[RecordWithListOfList].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        val l = record.field.map(_.asJava).asJava
        builder.set("field", l)

        Encoder.encode[RecordWithListOfList](record) should beRight(builder.build())
      }

    }

    "encode a record with a more nested list" in {
      forAll { list: List[Int] =>
        val record = RecordWithManyListsOfList(List(List(List(List(list)))))
        val schema = AvroSchema[RecordWithListOfList].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        val l = record.field.map(_.map(_.map(_.map(_.asJava).asJava).asJava).asJava).asJava
        builder.set("field", l)

        val result            = Encoder.encode[RecordWithManyListsOfList](record)
        val resultAsScalaList = result.map(_.get(0).asInstanceOf[java.util.List[Any]].asScala)
        resultAsScalaList should beRight(builder.build().get(0).asInstanceOf[java.util.List[Any]].asScala)
      }

    }

    "blah" in {

      val as = AvroSchema[TestRecord].schema.value

      val gr = new GenericData.Record(as)

      val arrSchema = as.getFields.asScala(0).schema()

      val ga = new GenericData.Array[Any](2, arrSchema)

      val l = List("cup", "cat", "ren", "dal")
      l.zipWithIndex.map {
        case (v, ind) =>
          ga.add(ind, v)
      }

      val x: GenericContainer = ga
      gr.put("field", x)

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
