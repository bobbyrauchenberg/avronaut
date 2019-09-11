package unit.encoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.encoder.Encoder
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import unit.common.UnitSpecBase

class SimpleRecordSpec extends UnitSpecBase {

  "encoder" should {
    "encode a case class with supported primitives to a suitable record" in {
      forAll { data: TestRecord =>
        val schema = AvroSchema[TestRecord].schema.value
        val expectedRecord = new GenericData.Record(schema)
        expectedRecord.put("string", data.string)
        expectedRecord.put("boolean", data.boolean)

        Encoder[TestRecord].encode(data, schema) shouldBe Right(expectedRecord)
      }
    }
  }

  case class TestRecord(string: String, boolean: Boolean)
}
