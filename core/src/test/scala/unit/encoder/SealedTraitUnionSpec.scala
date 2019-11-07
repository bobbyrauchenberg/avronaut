package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.encoder.SealedTraitUnionSpec._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class SealedTraitUnionSpec extends UnitSpecBase {

  "encoder" should {
    "encode a mixed sealed trait as a union" in new TestContext {
      forAll { record: XWrapper =>
        whenever(record.field == A) {

          val schema        = xSchema.data.value.schema
          val genericRecord = new GenericRecordBuilder(schema)

          record.field match {
            case A =>
              val s    = schema.getFields.asScala.head.schema().getTypes.asScala.head
              val enum = GenericData.get.createEnum(value.toString, s)
              genericRecord.set("field", enum)
            case B(field) =>
              val bSchema = AvroSchema.toSchema[B]
              val bGR     = new GenericData.Record(bSchema.data.value.schema)
              bGR.put(0, field)
              genericRecord.set("field", bGR)
          }

          Encoder.encode[XWrapper](record, xEncoder) should
            beRight(genericRecord.build.asInstanceOf[GenericRecord])

        }

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
