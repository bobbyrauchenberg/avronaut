package unit.common

import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import org.apache.avro.{Schema, SchemaBuilder}
import unit.common.common.UnitSpecBase
import SimpleRecords._

import scala.collection.JavaConverters._

class SimpleRecordsSpec extends UnitSpecBase {

  "decoder" should {
    "convert a record with a string field" in new TestContext {
      forAll { record: StringRecord =>
        val schema = SchemaBuilder.builder().stringBuilder().endString()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with a boolean field" in new TestContext {
      forAll { record: BooleanRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with an int field" in new TestContext {
      forAll { record: IntRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with a long field" in new TestContext {
      forAll { record: LongRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with a float field" in new TestContext {
      forAll { record: FloatRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with a double field" in new TestContext {
      forAll { record: DoubleRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }
    "convert a record with a byte array field" in new TestContext {
      forAll { record: BytesRecord =>
        val schema = SchemaBuilder.builder().intBuilder().endInt()
        val field = new Schema.Field("field", schema)
        runAssert(field, record.field, record)
      }
    }

  }


  trait TestContext {
    def runAssert[T, U: Decoder](field: Schema.Field, fieldValue: T, expected: U) = {

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

}
