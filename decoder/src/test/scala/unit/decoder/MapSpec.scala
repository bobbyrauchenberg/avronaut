package unit.decoder

import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._

import scala.collection.JavaConverters._
import RecordsWithMaps._
import com.rauchenberg.cupcatAvro.schema.AvroSchema

class MapSpec extends UnitSpecBase {

  "decoder" should {
    "convert a record with a map" in new TestContext {
      forAll { record: RecordWithMap =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a map with a default" in new TestContext {
      forAll { record: RecordWithDefaultMap =>
        runAssert(record.field, record)
      }
    }
  }

  trait TestContext {
    def runAssert[T, U: Decoder : AvroSchema](fieldValue: T, expected: U) = {

      val schema = AvroSchema[U].schema.right.get
      val field = new Schema.Field("field", schema)
      val recordSchema = Schema.createRecord(List(field).asJava)

      val record = new GenericData.Record(recordSchema)
      record.put("field", fieldValue)

      DecodeTo[U](record) should beRight(expected)
    }
  }

}

private [this] object RecordsWithMaps {
  case class RecordWithMap(field: Map[String, String])

  case class RecordWithDefaultMap(field: Map[String, String] = Map("cup" -> "cat"))
}
