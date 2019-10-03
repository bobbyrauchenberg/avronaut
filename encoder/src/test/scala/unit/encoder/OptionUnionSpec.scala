//package unit.encoder
//
//import collection.JavaConverters._
//import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
//import com.rauchenberg.avronaut.decoder.Decoder
//import com.rauchenberg.avronaut.encoder.Encoder
//import com.rauchenberg.avronaut.schema.AvroSchema
//import org.apache.avro.{Schema, SchemaBuilder}
//import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
//import unit.common.UnitSpecBase
//
//class OptionUnionSpec extends UnitSpecBase {
//
//  "encoder" should {
//
//    "encode a Union of null and T" in {
//      forAll { record: RecordWithUnion =>
//        val schema        = AvroSchema[RecordWithUnion].schema.value
//        val genericRecord = new GenericData.Record(schema)
//
//        genericRecord.put(0, record.field.orNull)
//
//        Encoder.encode[RecordWithUnion](record) should beRight(genericRecord)
//      }
//    }
//
//    "encode a Union of null and T roundtrip" in {
//      forAll { record: RecordWithUnion =>
//        Encoder.encode[RecordWithUnion](record).flatMap { v =>
//          Decoder.decode[RecordWithUnion](v)
//        } should beRight(record)
//      }
//    }
//
//    "encode a union with a record" in {
//      forAll { record: SimpleRecord =>
//        val simpleRecordSchema   = AvroSchema[SimpleRecord].schema.value
//        implicit val outerSchema = AvroSchema[RecordWithUnionOfCaseclass].schema.value
//
//        val unionSchema = Schema.createUnion(List(SchemaBuilder.builder.nullType, simpleRecordSchema): _*)
//
//        val innerSchema = unionSchema.getTypes.asScala.last
//        val innerRecord = new GenericData.Record(innerSchema)
//
//        innerRecord.put(0, record.cup)
//        innerRecord.put(1, record.cat)
//
//        val outerRecord   = new GenericData.Record(outerSchema)
//        val recordBuilder = new GenericRecordBuilder(outerRecord)
//
//        recordBuilder.set("field", innerRecord)
//
//        Encoder.encode[RecordWithUnionOfCaseclass](RecordWithUnionOfCaseclass(Some(record))) should beRight(
//          recordBuilder.build)
//      }
//    }
//
//    "encode a union with a record roundtrip" in {
//      forAll { record: RecordWithUnionOfCaseclass =>
//        Encoder.encode[RecordWithUnionOfCaseclass](record).flatMap { v =>
//          Decoder.decode[RecordWithUnionOfCaseclass](v)
//        } should beRight(record)
//      }
//    }
//
//    "encode a union with a list" in {
//      forAll { record: Option[List[String]] =>
//        implicit val schema = AvroSchema[RecordWithOptionalListCaseClass].schema.value
//
//        val builder = new GenericRecordBuilder(new GenericData.Record(schema))
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
//  }
//
//  case class RecordWithUnion(field: Option[String])
//  case class SimpleRecord(cup: String, cat: Int)
//  case class RecordWithUnionOfCaseclass(field: Option[SimpleRecord])
//  case class RecordWithOptionalListCaseClass(field: Option[List[String]])
//
//}
