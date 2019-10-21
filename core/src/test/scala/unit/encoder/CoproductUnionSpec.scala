//package unit.encoder
//
//import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
//import com.rauchenberg.avronaut.encoder.Encoder
//import com.rauchenberg.avronaut.schema.AvroSchema
//import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
//import shapeless.{:+:, CNil, Inl, Inr}
//import unit.utils.UnitSpecBase
//import RunRoundTripAssert._
//
//class CoproductUnionSpec extends UnitSpecBase {
//
//  "encoder" should {
//
//    "encode a union of multiple types" in {
//      forAll { (field: Long, field1: String :+: Boolean :+: Int :+: CNil, field2: Boolean) =>
//        implicit val schema = AvroSchema.toSchema[WriterRecordWithCoproduct]
//
//        val recordBuilder = new GenericRecordBuilder(new GenericData.Record(schema.data.value.schema))
//
//        field1 match {
//          case Inl(long)          => recordBuilder.set("field1", long)
//          case Inr(Inl(boolean))  => recordBuilder.set("field1", boolean)
//          case Inr(Inr(Inl(int))) => recordBuilder.set("field1", int)
//          case Inr(Inr(Inr(_)))   =>
//        }
//
//        recordBuilder.set("writerField", field)
//
//        recordBuilder.set("field2", field2)
//
//        val toEncode = WriterRecordWithCoproduct(field, field1, field2)
//        val expected = recordBuilder.build
//
//        Encoder.encode[WriterRecordWithCoproduct](toEncode) should beRight(expected)
//      }
//    }
//
//    "do a roundtrip encode and decode" in {
//      implicit val schema = AvroSchema.toSchema[WriterRecordWithCoproduct]
//      runRoundTrip[WriterRecordWithCoproduct]
//    }
//
//  }
//
//  type CP = String :+: Boolean :+: Int :+: CNil
//  case class WriterRecordWithCoproduct(writerField: Long, field1: CP, field2: Boolean)
//  case class ReaderRecordWithCoproduct(field2: Boolean, field1: CP)
//
//}
