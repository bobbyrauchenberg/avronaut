package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.schema.AvroSchema
import unit.utils.UnitSpecBase
import SealedTraitUnionSpec._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}

class SealedTraitUnionSpec extends UnitSpecBase {

  "encoder" should {
    "encode a mixed sealed trait as a union" in new TestContext {
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
        val res =
          Encoder.encode[XWrapper](record, xEncoder, xSchema.data) should
            beRight(genericRecord.build.asInstanceOf[GenericRecord])
      }
    }
  }

  trait TestContext {
    val xSchema  = AvroSchema.toSchema[XWrapper]
    val xEncoder = Encoder[XWrapper]
    val xDecoder = Decoder[XWrapper]
  }

}

object SealedTraitUnionSpec {

  sealed trait X
  case object A               extends X
  case class B(field: String) extends X

  case class XWrapper(field: X)

}
