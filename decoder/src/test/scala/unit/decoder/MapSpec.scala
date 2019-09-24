package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Parser
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}

import scala.collection.JavaConverters._
class MapSpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a map" in {

      forAll { record: RecordWithMap =>
        val schema = AvroSchema[RecordWithMap].schema

        val genericRecord = new GenericData.Record(schema.value)

        val recordBuilder = new GenericRecordBuilder(genericRecord)
        recordBuilder.set("field", record.field.asJava)

        Parser.decode[RecordWithMap](schema.value, recordBuilder.build()) should beRight(record)
      }
    }
  }

  case class RecordWithMap(field: Map[String, Boolean])
}
