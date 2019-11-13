package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
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

          val schema        = Codec.schema[XWrapper].value
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

          record.encode should beRight(genericRecord.build.asInstanceOf[GenericRecord])

        }

      }
    }

  }

  trait TestContext {
    implicit val codec: Codec[XWrapper] = Codec[XWrapper]
  }

}

object SealedTraitUnionSpec {

  sealed trait X
  case object A               extends X
  case class B(field: String) extends X

  case class XWrapper(field: X)

  sealed trait Foo
  case class Cup(cat: Int) extends Foo
  case class Bar(baz: Int) extends Foo

}
