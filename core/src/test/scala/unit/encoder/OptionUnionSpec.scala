package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import com.rauchenberg.avronaut.schema.AvroSchema._
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.apache.avro.{Schema, SchemaBuilder}
import unit.encoder.RunRoundTripAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class OptionUnionSpec extends UnitSpecBase {

  "encoder" should {

    "encode a Union of null and T" in {
      val writerSchema = AvroSchema.toSchema[RecordWithUnion].value
      forAll { record: RecordWithUnion =>
        val genericRecord = new GenericData.Record(writerSchema.schema)

        genericRecord.put(0, record.field.orNull)

        Encoder.encode[RecordWithUnion](record, writerSchema) should beRight(genericRecord)
      }
    }

    "encode a union with a record" in {
      val writerSchema = AvroSchema.toSchema[RecordWithUnionOfCaseclass].value

      forAll { record: SimpleRecord =>
        val simpleRecordSchema = AvroSchema.toSchema[SimpleRecord].value
        val unionSchema        = Schema.createUnion(List(SchemaBuilder.builder.nullType, simpleRecordSchema.schema): _*)

        val innerSchema = unionSchema.getTypes.asScala.last
        val innerRecord = new GenericData.Record(innerSchema)

        innerRecord.put(0, record.cup)
        innerRecord.put(1, record.cat)

        val outerRecord   = new GenericData.Record(writerSchema.schema)
        val recordBuilder = new GenericRecordBuilder(outerRecord)

        recordBuilder.set("field", innerRecord)

        Encoder
          .encode[RecordWithUnionOfCaseclass](RecordWithUnionOfCaseclass(Some(record)), writerSchema) should beRight(
          recordBuilder.build)
      }
    }

    "encode a union with a list" in {
      implicit val writerSchema = AvroSchema.toSchema[RecordWithOptionalListCaseClass].value
      forAll { record: Option[List[String]] =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        val r = RecordWithOptionalListCaseClass(record)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Encoder.encode[RecordWithOptionalListCaseClass](r, writerSchema) should beRight(builder.build())
      }
    }

    "encode a union with a list roundtrip" in {
      runRoundTrip[RecordWithUnion]
      runRoundTrip[RecordWithUnionOfCaseclass]
      runRoundTrip[RecordWithUnionOfCaseclass]
    }
  }

  case class RecordWithUnion(field: Option[String])
  case class SimpleRecord(cup: String, cat: Int)
  case class RecordWithUnionOfCaseclass(field: Option[SimpleRecord])
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])

}
