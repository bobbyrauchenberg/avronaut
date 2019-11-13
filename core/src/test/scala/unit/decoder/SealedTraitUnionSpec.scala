package unit.decoder

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.SealedTraitUnionSpec.{A, B, XWrapper}
import unit.utils.UnitSpecBase

class SealedTraitUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode a sealed trait union" in {
      implicit val codec = Codec[XWrapper]
      val schema         = Codec.schema[XWrapper].value

      forAll { record: XWrapper =>
        val genericRecord = new GenericRecordBuilder(schema)

        record.field match {
          case A => genericRecord.set("field", record.field.toString)
          case B(field) =>
            val bSchema = AvroSchema.toSchema[B]
            val bGR     = new GenericData.Record(bSchema.data.value.schema)
            bGR.put(0, field)
            genericRecord.set("field", bGR)
        }

        genericRecord.build.decode[XWrapper] should beRight(record)
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
