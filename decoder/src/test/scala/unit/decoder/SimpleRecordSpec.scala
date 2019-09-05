package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import com.rauchenberg.cupcatAvro.schema.AvroSchema
import org.apache.avro.generic.GenericData
import org.apache.avro.Schema
import unit.common.UnitSpecBase
import SimpleRecords._

import scala.collection.JavaConverters._

class SimpleRecordSpec extends UnitSpecBase {

  "decoder" should {
    "convert a record with a string field" in new TestContext {
      forAll { record: StringRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a boolean field" in new TestContext {
      forAll { record: BooleanRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with an int field" in new TestContext {
      forAll { record: IntRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a long field" in new TestContext {
      forAll { record: LongRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a float field" in new TestContext {
      forAll { record: FloatRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a double field" in new TestContext {
      forAll { record: DoubleRecord =>
        runAssert(record.field, record)
      }
    }
    "convert a record with a byte array field" in new TestContext {
      forAll { record: BytesRecord =>
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

private[this] object SimpleRecords {

  case class BooleanRecord(field: Boolean)

  case class IntRecord(field: Int)

  case class LongRecord(field: Long)

  case class FloatRecord(field: Float)

  case class DoubleRecord(field: Double)

  case class StringRecord(field: String)

  case class BytesRecord(field: Array[Byte])

  case class NestedRecord(field: IntRecord)

}
