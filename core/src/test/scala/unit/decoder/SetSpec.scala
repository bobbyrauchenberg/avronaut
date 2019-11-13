package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.encoder.EncoderBuilder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import collection.JavaConverters._

class SetSpec extends UnitSpecBase {

  "decoder" should {
    "decode a set" in new TestContext {
      forAll { record: RecordWithSet =>
        val genericRecordBuilder = new GenericRecordBuilder(schema)
        val expected             = genericRecordBuilder.set("field", record.field.toList.asJava).build.asInstanceOf[GenericRecord]
        expected.decode[RecordWithSet] should beRight(record)
      }
    }
  }

  case class RecordWithSet(field: Set[Int])

  trait TestContext {
    val recordWithSetEncoder                 = EncoderBuilder[RecordWithSet]
    implicit val codec: Codec[RecordWithSet] = Codec[RecordWithSet]
    val schema                               = Codec.schema[RecordWithSet].value
  }

}
