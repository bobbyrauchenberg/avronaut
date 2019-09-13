package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Parser
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.utils.RunAssert._

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  "decoder" should {
    "decode a record with a list" in {
      forAll { record: RecordWithList =>
        whenever(!record.field.isEmpty) {
          runListAssert(record.field, record)
        }
      }
    }
    "decode a record with a seq" in {
      forAll { record: RecordWithSeq =>
        runListAssert(record.field, record)
      }
    }
    "decode a record with a vector" in {
      forAll { record: RecordWithSeq =>
        runListAssert(record.field, record)
      }
    }
    "decode a record with a list and a default value" in {
      val record = RecordWithListDefault()
      runListAssert(record.field, record)
    }
    "decode a record with a seq and a default value" in {
      val record = RecordWithSeqDefault()
      runListAssert(record.field, record)
    }
    "decode a record with a vector and a default value" in {
      val record = RecordWithVectorDefault()
      runListAssert(record.field, record)
    }

    "decode a record with a list of simple caseclass" in {
      RecordWithListOfSimpleCaseClass(List(InnerNested("rendal", 5), InnerNested("cuppers", 5)))

      val rootSchema  = AvroSchema[RecordWithListOfSimpleCaseClass].schema
      val innerSchema = AvroSchema[InnerNested].schema

      val innerRecord1 = new GenericData.Record(innerSchema.value)
      innerRecord1.put(0, "cup")
      innerRecord1.put(1, 5)

      val innerRecord2 = new GenericData.Record(innerSchema.value)
      innerRecord2.put(0, "cat")
      innerRecord2.put(1, 10)

      val rootRecord = new GenericData.Record(rootSchema.value)

      val recordBuilder = new GenericRecordBuilder(rootRecord)
      recordBuilder.set("field", List(innerRecord1, innerRecord2))
    }

    "decode a record with a list of caseclass" in {
      forAll { r: RecordWithListOfCaseClass =>
        val rootSchema  = AvroSchema[RecordWithListOfCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.zipWithIndex.map {
          case (outer, _) =>
            val outerRecord = new GenericData.Record(outerSchema.value)
            val innerRecord = new GenericData.Record(innerSchema.value)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Parser.decode[RecordWithListOfCaseClass](rootSchema.value, recordBuilder.build()) should beRight(r)
      }
    }

    "decode a record with a seq of caseclass" in {
      forAll { r: RecordWithSeqOfCaseClass =>
        val rootSchema  = AvroSchema[RecordWithSeqOfCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            val outerRecord = new GenericData.Record(outerSchema.value)
            val innerRecord = new GenericData.Record(innerSchema.value)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Parser.decode[RecordWithSeqOfCaseClass](rootSchema.value, recordBuilder.build()) should beRight(r)
      }
    }
    "decode a record with a vector of caseclass" in {
      forAll { r: RecordWithVectorOfCaseClass =>
        val rootSchema  = AvroSchema[RecordWithVectorOfCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            val outerRecord = new GenericData.Record(outerSchema.value)
            val innerRecord = new GenericData.Record(innerSchema.value)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Parser.decode[RecordWithVectorOfCaseClass](rootSchema.value, recordBuilder.build()) should beRight(r)
      }
    }

    "decode a record with a list of optional caseclass" in {
      forAll { r: RecordWithListOfOptionalCaseClass =>
        val rootSchema  = AvroSchema[RecordWithListOfOptionalCaseClass].schema
        val outerSchema = AvroSchema[Nested].schema
        val innerSchema = AvroSchema[InnerNested].schema

        val rootRecord = new GenericData.Record(rootSchema.value)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            outer match {
              case None =>
                null
              case Some(v) =>
                val outerRecord = new GenericData.Record(outerSchema.value)
                val innerRecord = new GenericData.Record(innerSchema.value)
                innerRecord.put(0, v.field2.field1)
                innerRecord.put(1, v.field2.field2)

                outerRecord.put(0, v.field1)
                outerRecord.put(1, innerRecord)
                outerRecord.put(2, v.field3)
                outerRecord
            }

        }.asJava
        recordBuilder.set("field", recordList)
        Parser.decode[RecordWithListOfOptionalCaseClass](rootSchema.value, recordBuilder.build()) should beRight(r)
      }
    }

  }

  case class RecordWithList(field: List[String])
  case class RecordWithSeq(field: Seq[String])
  case class RecordWithVector(field: Vector[String])
  case class RecordWithListDefault(field: List[String] = List("cup", "cat"))
  case class RecordWithSeqDefault(field: Seq[String] = Seq("cup", "cat"))
  case class RecordWithVectorDefault(field: Vector[String] = Vector("cup", "cat"))

  case class InnerNested(field1: String, field2: Int)
  case class Nested(field1: String, field2: InnerNested, field3: Int)

  case class RecordWithListOfSimpleCaseClass(field: List[InnerNested])

  case class RecordWithListOfCaseClass(field: List[Nested])
  case class RecordWithSeqOfCaseClass(field: Seq[Nested])
  case class RecordWithVectorOfCaseClass(field: Vector[Nested])

  case class RecordWithListOfOptionalCaseClass(field: List[Option[Nested]])

}
