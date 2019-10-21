package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import org.apache.avro.generic.{GenericData, GenericRecord}
import unit.encoder.SealedTraitEnumSpec.{EnumRecord, SealedTraitEnumWithDefault}
import unit.utils.UnitSpecBase

class SealedTraitEnumSpec extends UnitSpecBase {

  "encoder" should {

    "handle sealed trait enums" in new TestContext {
      forAll { record: EnumRecord =>
        val schema = Codec.schema[EnumRecord].value
        val gr     = new GenericData.Record(schema)
        gr.put(0, record.field.toString)

        record.encode should beRight(gr.asInstanceOf[GenericRecord])
      }
    }

    "handle sealed trait enums with defaults" in new TestContext {
      val record = SealedTraitEnumWithDefault()
      val schema = Codec.schema[SealedTraitEnumWithDefault].value
      val gr     = new GenericData.Record(schema)
      gr.put(0, record.field.toString)

      SealedTraitEnumWithDefault().encode should beRight(gr.asInstanceOf[GenericRecord])
    }

  }

  trait TestContext {
    implicit val enumRecordCodec: Codec[EnumRecord]                                 = Codec[EnumRecord]
    implicit val sealedTraitEnumWithDefaultCodec: Codec[SealedTraitEnumWithDefault] = Codec[SealedTraitEnumWithDefault]
  }

}

object SealedTraitEnumSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class EnumRecord(field: A)

  case class SealedTraitEnumWithDefault(field: A = B)

}
