package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class SetSpec extends UnitSpecBase {

  "encoder" should {
    "encode sets" in new TestContext {
      forAll { record: RecordWithSet =>
        val genericRecordBuilder = new GenericRecordBuilder(recordWithSetSchemaData.value.schema)
        val expected             = genericRecordBuilder.set("field", record.field.asJava).build
        Encoder.encode[RecordWithSet](record, recordWithSetEncoder) should
          beRight(expected.asInstanceOf[GenericRecord])
      }
    }

  }

  case class RecordWithSet(field: Set[Int])

  trait TestContext {
    val recordWithSetEncoder    = Encoder[RecordWithSet]
    val recordWithSetSchemaData = AvroSchema.toSchema[RecordWithSet].data
  }

}
