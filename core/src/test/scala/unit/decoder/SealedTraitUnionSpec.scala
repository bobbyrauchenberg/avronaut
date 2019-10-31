package unit.decoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.SealedTraitUnionSpec.{A, B, XWrapper}
import unit.utils.UnitSpecBase

class SealedTraitUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode a sealed trait union" in {
      val xSchema  = AvroSchema.toSchema[XWrapper]
      val xDecoder = Decoder[XWrapper]

      forAll { record: XWrapper =>
        val genericRecord = new GenericRecordBuilder(xSchema.data.value.schema)

        record.field match {
          case A => genericRecord.set("field", record.field.toString)
          case B(field) =>
            val bSchema = AvroSchema.toSchema[B]
            val bGR     = new GenericData.Record(bSchema.data.value.schema)
            bGR.put(0, field)
            genericRecord.set("field", bGR)
        }

        Decoder.decode[XWrapper](genericRecord.build, xDecoder) should beRight(record)
      }
    }
  }

}

object SealedTraitUnionSpec {

  sealed trait X
  case object A               extends X
  case class B(field: String) extends X

  case class XWrapper(field: X)

}
