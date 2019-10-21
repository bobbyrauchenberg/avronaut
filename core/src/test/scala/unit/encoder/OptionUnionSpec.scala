package unit.encoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import org.apache.avro.{Schema, SchemaBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class OptionUnionSpec extends UnitSpecBase {

  "encoder" should {

    "encode a Union of null and T" in new TestContext {
      forAll { record: RecordWithUnion =>
        val schema        = Codec.schema[RecordWithUnion].value
        val genericRecord = new GenericData.Record(schema)

        genericRecord.put(0, record.field.orNull)

        record.encode should beRight(genericRecord.asInstanceOf[GenericRecord])
      }
    }

    "encode a union with a record" in new TestContext {
      forAll { record: SimpleRecord =>
        val schema             = Codec.schema[RecordWithUnionOfCaseclass].value
        val simpleRecordSchema = AvroSchema.toSchema[SimpleRecord].data.value
        val unionSchema        = Schema.createUnion(List(SchemaBuilder.builder.nullType, simpleRecordSchema.schema): _*)

        val innerSchema = unionSchema.getTypes.asScala.last
        val innerRecord = new GenericData.Record(innerSchema)

        innerRecord.put(0, record.cup)
        innerRecord.put(1, record.cat)

        val outerRecord   = new GenericData.Record(schema)
        val recordBuilder = new GenericRecordBuilder(outerRecord)

        recordBuilder.set("field", innerRecord)

        RecordWithUnionOfCaseclass(Some(record)).encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a union with a list" in new TestContext {
      forAll { record: Option[List[String]] =>
        val schema  = Codec.schema[RecordWithOptionalListCaseClass].value
        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        val r = RecordWithOptionalListCaseClass(record)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        r.encode should beRight(builder.build.asInstanceOf[GenericRecord])
      }
    }
  }

  case class RecordWithUnion(field: Option[String])
  case class SimpleRecord(cup: String, cat: Int)
  case class RecordWithUnionOfCaseclass(field: Option[SimpleRecord])
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])

  trait TestContext {
    implicit val recordWithUnionCodec: Codec[RecordWithUnion]                       = Codec[RecordWithUnion]
    implicit val recordWithUnionOfCaseClassCodec: Codec[RecordWithUnionOfCaseclass] = Codec[RecordWithUnionOfCaseclass]
    implicit val unionWithListCodec: Codec[RecordWithOptionalListCaseClass]         = Codec[RecordWithOptionalListCaseClass]
  }
}
