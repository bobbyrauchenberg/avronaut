package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord}
import SealedTraitSpec.{EnumRecord, SealedTraitEnumWithDefault}
import unit.utils.UnitSpecBase

class SealedTraitSpec extends UnitSpecBase {

  "encoder" should {

    "handle sealed trait enums" in new TestContext {
      forAll { record: EnumRecord =>
        val gr = new GenericData.Record(enumRecordSchema.data.value.schema)
        gr.put(0, record.field.toString)

        Encoder.encode(record, enumRecordEncoder, enumRecordSchema.data) should beRight(gr.asInstanceOf[GenericRecord])
      }
    }

    "handle sealed trait enums with defaults" in new TestContext {
      val record = SealedTraitEnumWithDefault()
      val gr     = new GenericData.Record(sealedTraitEnumWithDefaultSchema.data.value.schema)
      gr.put(0, record.field.toString)

      Encoder.encode(
        SealedTraitEnumWithDefault(),
        sealedTraitEnumWithDefaultEncoder,
        sealedTraitEnumWithDefaultSchema.data
      ) should beRight(gr.asInstanceOf[GenericRecord])
    }

  }

  trait TestContext {
    implicit val enumRecordEncoder = Encoder[EnumRecord]
    implicit val enumRecordSchema  = AvroSchema.toSchema[EnumRecord]

    implicit val sealedTraitEnumWithDefaultEncoder = Encoder[SealedTraitEnumWithDefault]
    implicit val sealedTraitEnumWithDefaultSchema =
      AvroSchema.toSchema[SealedTraitEnumWithDefault]
  }

}

object SealedTraitSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class EnumRecord(field: A)

  case class SealedTraitEnumWithDefault(field: A = B)

}
