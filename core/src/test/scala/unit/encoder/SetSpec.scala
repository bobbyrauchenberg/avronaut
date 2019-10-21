package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class SetSpec extends UnitSpecBase {

  "encoder" should {
    "encode sets" in new TestContext {
      forAll { record: RecordWithSet =>
        val schema               = Codec.schema[RecordWithSet].value
        val genericRecordBuilder = new GenericRecordBuilder(schema)
        val expected             = genericRecordBuilder.set("field", record.field.asJava).build
        record.encode should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

  }

  case class RecordWithSet(field: Set[Int])

  trait TestContext {
    implicit val codec: Codec[RecordWithSet] = Codec[RecordWithSet]
  }

}
