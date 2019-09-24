package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.{Decoder, Parser}
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.apache.avro.{Schema, SchemaBuilder}
import unit.decoder.utils.RunAssert._

import scala.collection.JavaConverters._

class OptionUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode an union of null and T" in {
      forAll { u: Union =>
        runAssert(u.field.getOrElse(null), u)
      }
    }

    "decode a union with a record" in {
      forAll { (s: SimpleRecord) =>
        val simpleRecordSchema = AvroSchema[SimpleRecord].schema.value
        val outerSchema        = AvroSchema[UnionRecord].schema.value

        val unionSchema = Schema.createUnion(List(SchemaBuilder.builder.nullType, simpleRecordSchema): _*)

        val innerSchema = unionSchema.getTypes.asScala.last
        val innerRecord = new GenericData.Record(innerSchema)

        innerRecord.put(0, s.cup)
        innerRecord.put(1, s.cat)

        val outerRecord   = new GenericData.Record(outerSchema)
        val recordBuilder = new GenericRecordBuilder(outerRecord)

        recordBuilder.set("field", innerRecord)

        Parser.decode[UnionRecord](outerSchema, recordBuilder.build) should beRight(
          UnionRecord(Some(SimpleRecord(s.cup, s.cat))))
      }

    }

    "decode a union to None when the record value is null" in {
      val unionSchema = AvroSchema[Union].schema.value

      val record = new GenericData.Record(unionSchema)
      record.put("field", null)

      Parser.decode[Union](unionSchema, record) should beRight(Union(None))
    }

    "decode a union with a default" in new TestContext {
      assertHasDefault[UnionWithDefault](UnionWithDefault())
    }

    "decode an option with a record default" in new TestContext {
      assertHasDefault[UnionWithCaseClassDefault](UnionWithCaseClassDefault())
    }

    "decode an union with null as default" in new TestContext {
      assertHasDefault[UnionWithNoneAsDefault](UnionWithNoneAsDefault(None))
    }

    "decode a union with a list of records" in {
      forAll { record: Option[List[String]] =>
        val schema = AvroSchema[RecordWithOptionalListCaseClass].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        val r = RecordWithOptionalListCaseClass(record)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Parser.decode[RecordWithOptionalListCaseClass](schema, builder.build()) should beRight(r)
      }
    }

    import OptionUnionSpec._

    "decode a union of null and enum" in {
      forAll { record: WriterRecordWithEnum =>
        val writerSchema = AvroSchema[WriterRecordWithEnum].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithEnum].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema))

        record.field1 match {
          case None       => builder.set("field1", null)
          case Some(enum) => builder.set("field1", enum.toString)
        }
        builder.set("writerField", record.writerField)
        builder.set("field2", record.field2)

        val expected = ReaderRecordWithEnum(record.field2, record.field1)

        Parser.decode[ReaderRecordWithEnum](readerSchema, builder.build()) should beRight(expected)
      }
    }

    trait TestContext {
      def assertHasDefault[T : AvroSchema : Decoder](expected: T) = {
        val schema = AvroSchema[T].schema.value

        val record = new GenericData.Record(schema)
        record.put("field", "value i don't recognise")

        Parser.decode[T](schema, record) should beRight(expected)
      }

    }

  }

  case class Union(field: Option[Int])

  case class SimpleRecord(cup: String, cat: Int)
  case class UnionRecord(field: Option[SimpleRecord])

  case class UnionWithDefault(field: Option[Int] = Option(123))

  case class DefaultValue(cup: String, cat: String)

  case class UnionWithNoneAsDefault(field: Option[DefaultValue] = None)

  case class UnionWithCaseClassDefault(field: Option[DefaultValue] = Option(DefaultValue("cup", "cat")))
  case class RecordWithOptionalListCaseClass(field: Option[List[String]])

}

private[this] object OptionUnionSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithEnum(field1: Option[A], writerField: String, field2: Boolean)
  case class ReaderRecordWithEnum(field2: Boolean, field1: Option[A])
}
