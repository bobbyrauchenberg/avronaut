package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.GenericData
import SealedTraitSpec.{EnumRecord, SealedTraitEnumWithDefault}
import unit.utils.UnitSpecBase

class SealedTraitSpec extends UnitSpecBase {

  "encoder" should {

    "handle sealed trait enums" in {
      forAll { record: EnumRecord =>
        val schema           = AvroSchema.toSchema[EnumRecord].value
        implicit val encoder = Encoder[EnumRecord]

        val gr = new GenericData.Record(schema.schema)
        gr.put(0, record.field.toString)

        Encoder.encode(record, schema) should beRight(gr)
      }
    }

    "handle sealed trait enums with defaults" in {
      val record           = SealedTraitEnumWithDefault()
      val schema           = AvroSchema.toSchema[SealedTraitEnumWithDefault].value
      implicit val encoder = Encoder[SealedTraitEnumWithDefault]

      val gr = new GenericData.Record(schema.schema)
      gr.put(0, record.field.toString)

      Encoder.encode(SealedTraitEnumWithDefault(), schema) should beRight(gr)
    }

  }

}

private[this] object SealedTraitSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class EnumRecord(field: A)

  case class SealedTraitEnumWithDefault(field: A = B)

}
