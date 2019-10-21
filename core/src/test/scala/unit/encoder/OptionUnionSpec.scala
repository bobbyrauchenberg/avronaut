//package unit.encoder
//
//import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
//import com.rauchenberg.avronaut.encoder.Encoder
//import com.rauchenberg.avronaut.schema.AvroSchema
//import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
//import org.apache.avro.{Schema, SchemaBuilder}
//import unit.encoder.RunRoundTripAssert._
//import unit.utils.UnitSpecBase
//
//import scala.collection.JavaConverters._
//
//class OptionUnionSpec extends UnitSpecBase {
//
//  "encoder" should {
//
//    "encode a Union of null and T" in new TestContext {
//      forAll { record: RecordWithUnion =>
//        val genericRecord = new GenericData.Record(recordWithUnionSchema.data.value.schema)
//
//        genericRecord.put(0, record.field.orNull)
//
//        Encoder.encode[RecordWithUnion](record) should beRight(genericRecord)
//      }
//    }
//
//    "encode a union with a record" in new TestContext {
//      forAll { record: SimpleRecord =>
//        val simpleRecordSchema = AvroSchema.toSchema[SimpleRecord].data.value
//        val unionSchema        = Schema.createUnion(List(SchemaBuilder.builder.nullType, simpleRecordSchema.schema): _*)
//
//        val innerSchema = unionSchema.getTypes.asScala.last
//        val innerRecord = new GenericData.Record(innerSchema)
//
//        innerRecord.put(0, record.cup)
//        innerRecord.put(1, record.cat)
//
//        val outerRecord   = new GenericData.Record(recordWithUnionOfCaseClassSchema.data.value.schema)
//        val recordBuilder = new GenericRecordBuilder(outerRecord)
//
//        recordBuilder.set("field", innerRecord)
//
//        Encoder
//          .encode[RecordWithUnionOfCaseclass](RecordWithUnionOfCaseclass(Some(record))) should beRight(
//          recordBuilder.build)
//      }
//    }
//
//    "encode a union with a list" in new TestContext {
//      forAll { record: Option[List[String]] =>
//        val builder = new GenericRecordBuilder(new GenericData.Record(unionWithListSchema.data.value.schema))
//
//        val r = RecordWithOptionalListCaseClass(record)
//
//        r.field match {
//          case Some(list) => builder.set("field", list.asJava)
//          case None       => builder.set("field", null)
//        }
//
//        Encoder.encode[RecordWithOptionalListCaseClass](r) should beRight(builder.build())
//      }
//    }
//
//    "encode a union with a list roundtrip" in new TestContext {
//      runRoundTrip[RecordWithUnion]
//      runRoundTrip[RecordWithUnionOfCaseclass]
//      runRoundTrip[RecordWithUnionOfCaseclass]
//    }
//  }
//
//  case class RecordWithUnion(field: Option[String])
//  case class SimpleRecord(cup: String, cat: Int)
//  case class RecordWithUnionOfCaseclass(field: Option[SimpleRecord])
//  case class RecordWithOptionalListCaseClass(field: Option[List[String]])
//
//  trait TestContext {
//    implicit val recordWithUnionSchema: AvroSchema[RecordWithUnion] = AvroSchema.toSchema[RecordWithUnion]
//    implicit val recordWithUnionOfCaseClassSchema: AvroSchema[RecordWithUnionOfCaseclass] =
//      AvroSchema.toSchema[RecordWithUnionOfCaseclass]
//    implicit val unionWithListSchema: AvroSchema[RecordWithOptionalListCaseClass] =
//      AvroSchema.toSchema[RecordWithOptionalListCaseClass]
//  }
//}
