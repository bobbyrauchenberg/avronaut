package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder

import collection.JavaConverters._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.common.UnitSpecBase

class ArrayEncoderSpec extends UnitSpecBase {

  "encoder" should {

    "encode a record with a list of primitives" in {
      forAll { record: TestRecord =>
        val schema   = AvroSchema[TestRecord].schema.value
        val expected = new GenericRecordBuilder(new GenericData.Record(schema))
        expected.set("string", record.string)
        expected.set("boolean", record.boolean)
        expected.set("int", record.int)
        expected.set("long", record.long)
        expected.set("list", record.list.asJava)
        expected.set("float", record.float)

        Encoder.encode[TestRecord](record) should beRight(expected.build.asInstanceOf[GenericRecord])
      }
    }

  }

  case class TestRecord(string: String, boolean: Boolean, int: Int, long: Long, list: List[String], float: Float)

}
