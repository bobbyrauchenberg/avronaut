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

    "decode an option with a simple record" in {
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

    "decode an option with a default" in new TestContext {
      assertHasDefault[UnionWithDefault](UnionWithDefault())
    }

    "decode an option with a caseclass default" in new TestContext {
      assertHasDefault[UnionWithCaseClassDefault](UnionWithCaseClassDefault())
    }

    "decode an option with none as default" in new TestContext {
      assertHasDefault[UnionWithNoneAsDefault](UnionWithNoneAsDefault(None))
    }

    "decode a record with an optional list of caseclass" in {
      forAll { value: Option[List[String]] =>
        val schema = AvroSchema[RecordWithOptionalListCaseClass].schema.value
        val record = new GenericData.Record(schema)

        val builder = new GenericRecordBuilder(record)

        val r = RecordWithOptionalListCaseClass(value)
        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        Parser.decode[RecordWithOptionalListCaseClass](schema, builder.build()) should beRight(r)
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
