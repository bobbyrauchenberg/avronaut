package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.cupcatAvro.decoder.{DecodeTo, Decoder}
import org.apache.avro.generic.GenericData
import org.apache.avro.{Schema, SchemaBuilder}
import unit.common.UnitSpecBase
import RecordsWithArrays._

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  "decoder" should {
    "convert a record with a list" in new TestContext {
      forAll { record: RecordWithList =>
        val schema = SchemaBuilder.builder().stringBuilder().endString()
        val field = new Schema.Field("field", schema)

        runAssert(field, record.field, record)
      }
    }
    "convert a record with a seq" in new TestContext {
      forAll { record: RecordWithSeq =>
        val schema = SchemaBuilder.builder().stringBuilder().endString()
        val field = new Schema.Field("field", schema)

        runAssert(field, record.field, record)
      }
    }
    "convert a record with a vector" in new TestContext {
      forAll { record: RecordWithSeq =>
        val schema = SchemaBuilder.builder().stringBuilder().endString()
        val field = new Schema.Field("field", schema)

        runAssert(field, record.field, record)
      }
    }
    "convert a record with a list and a default value" in new TestContext {
      val record = RecordWithListDefault()
      val schema = SchemaBuilder.builder().stringBuilder().endString()
      val field = new Schema.Field("field", schema)

      runAssert(field, record.field, record)
    }
    "convert a record with a seq and a default value" in new TestContext {
      val record = RecordWithSeqDefault()
      val schema = SchemaBuilder.builder().stringBuilder().endString()
      val field = new Schema.Field("field", schema)

      runAssert(field, record.field, record)
    }
    "convert a record with a vector and a default value" in new TestContext {
      val record = RecordWithVectorDefault()
      val schema = SchemaBuilder.builder().stringBuilder().endString()
      val field = new Schema.Field("field", schema)

      runAssert(field, record.field, record)
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

private[this] object RecordsWithArrays {

  case class RecordWithList(field: List[String])

  case class RecordWithSeq(field: Seq[String])

  case class RecordWithVector(field: Vector[String])

  case class RecordWithListDefault(field: List[String] = List("cup", "cat"))

  case class RecordWithSeqDefault(field: Seq[String] = Seq("cup", "cat"))

  case class RecordWithVectorDefault(field: Vector[String] = Vector("cup", "cat"))

}