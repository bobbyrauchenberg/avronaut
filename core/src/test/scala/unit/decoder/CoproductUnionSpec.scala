package unit.decoder

import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import shapeless.{:+:, CNil, Inl, Inr}
import unit.utils.UnitSpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator._

class CoproductUnionSpec extends UnitSpecBase {

  "decoder" should {

    "decode a union of multiple types" in {
      forAll { (writerField: Long, field1: String :+: Boolean :+: Simple :+: Int :+: CNil, field2: Boolean) =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithCoproduct].data.value
        val simpleSchema = AvroSchema.toSchema[Simple].data.value
        val decoder      = Decoder[ReaderRecordWithCoproduct]

        val recordBuilder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))
        val simpleGenRec  = new GenericData.Record(simpleSchema.schema)

        field1 match {
          case Inl(string)       => recordBuilder.set("field1", string)
          case Inr(Inl(boolean)) => recordBuilder.set("field1", boolean)
          case Inr(Inr(Inl(Simple(i)))) =>
            simpleGenRec.put(0, i)
            recordBuilder.set("field1", simpleGenRec)
          case Inr(Inr(Inr(Inl(int)))) => recordBuilder.set("field1", int)
          case Inr(Inr(Inr(Inr(_))))   =>
        }

        recordBuilder.set("writerField", writerField)

        recordBuilder.set("field2", field2)

        val expected = ReaderRecordWithCoproduct(field2, field1)

        Decoder.decode[ReaderRecordWithCoproduct](recordBuilder.build, decoder) should beRight(expected)
      }
    }

  }

  type CP = String :+: Boolean :+: Simple :+: Int :+: CNil
  case class Simple(field: Int)
  case class WriterRecordWithCoproduct(writerField: Long, field1: CP, field2: Boolean)
  case class ReaderRecordWithCoproduct(field2: Boolean, field1: CP)

}
