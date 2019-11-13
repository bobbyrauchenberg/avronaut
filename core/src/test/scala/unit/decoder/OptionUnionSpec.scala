package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import org.apache.avro.{Schema, SchemaBuilder}
import unit.utils.RunAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class OptionUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode an union of null and T" in {
      forAll { record: Union =>
        implicit val codec = Codec[Union]
        runDecodeAssert(record.field.getOrElse(null), record)
      }
    }

    "decode a union with a record" in {
      forAll { record: SimpleRecord =>
        val writerSchema = AvroSchema.toSchema[SimpleRecord].data.value

        implicit val outerSchema = AvroSchema.toSchema[UnionRecord]
        implicit val codec       = Codec[UnionRecord]

        val unionSchema = Schema.createUnion(List(SchemaBuilder.builder.nullType, writerSchema.schema): _*)

        val innerSchema = unionSchema.getTypes.asScala.last
        val innerRecord = new GenericData.Record(innerSchema)

        innerRecord.put(0, record.cup)
        innerRecord.put(1, record.cat)

        val outerRecord   = new GenericData.Record(outerSchema.data.value.schema)
        val recordBuilder = new GenericRecordBuilder(outerRecord)

        recordBuilder.set("field", innerRecord)

        recordBuilder.build.decode[UnionRecord] should beRight(UnionRecord(Some(SimpleRecord(record.cup, record.cat))))
      }

    }

    "decode a union to None when the record value is null" in {
      implicit val unionSchema = AvroSchema.toSchema[Union]
      implicit val codec       = Codec[Union]

      val record = new GenericData.Record(unionSchema.data.value.schema)
      record.put("field", null)

      record.decode[Union] should beRight(Union(None))
    }

    "decode a union with a default" in new TestContext {
      implicit val codec = Codec[UnionWithDefault]
      assertHasDefault[UnionWithDefault](UnionWithDefault())
    }

    "decode an option with a record default" in new TestContext {
      implicit val codec = Codec[UnionWithCaseClassDefault]
      assertHasDefault[UnionWithCaseClassDefault](UnionWithCaseClassDefault())
    }

    "decode an union with null as default" in new TestContext {
      implicit val codec = Codec[UnionWithNoneAsDefault]
      assertHasDefault[UnionWithNoneAsDefault](UnionWithNoneAsDefault(None))
    }

    "decode a union with a list of records" in {
      forAll { writerRecord: Option[List[String]] =>
        val writerSchema   = AvroSchema.toSchema[RecordWithOptionalListCaseClass].data.value
        implicit val codec = Codec[RecordWithOptionalListCaseClass]

        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        val r = RecordWithOptionalListCaseClass(writerRecord)

        r.field match {
          case Some(list) => builder.set("field", list.asJava)
          case None       => builder.set("field", null)
        }

        builder.build.decode[RecordWithOptionalListCaseClass] should beRight(r)
      }
    }

    import OptionUnionSpec._

    "decode a union of null and enum" in {
      forAll { writerRecord: WriterRecordWithEnum =>
        val writerSchema   = AvroSchema.toSchema[WriterRecordWithEnum].data.value
        implicit val codec = Codec[ReaderRecordWithEnum]

        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema.schema))

        writerRecord.field1 match {
          case None       => builder.set("field1", null)
          case Some(enum) => builder.set("field1", enum.toString)
        }
        builder.set("writerField", writerRecord.writerField)
        builder.set("field2", writerRecord.field2)

        val expected = ReaderRecordWithEnum(writerRecord.field2, writerRecord.field1)

        builder.build.decode[ReaderRecordWithEnum] should beRight(expected)
      }
    }

    trait TestContext {
      def assertHasDefault[B](expected: B)(implicit codec: Codec[B]) = {

        val schema = Codec.schema[B].value

        val record = new GenericData.Record(schema)
        record.put("field", "value i don't recognise")

        record.decode[B] should beRight(expected)
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
