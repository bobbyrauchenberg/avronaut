package unit.decoder

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.utils.RunAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class ArraySpec extends UnitSpecBase {

  import ArraySpec._

  "decoder" should {
    "decode a record with a list" in {
      forAll { record: RecordWithList =>
        runListAssert(record.field, record)
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
        Decoder.decode[RecordWithListOfCaseClass](recordBuilder.build()) should beRight(r)
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
        Decoder.decode[RecordWithSeqOfCaseClass](recordBuilder.build()) should beRight(r)
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
        Decoder.decode[RecordWithVectorOfCaseClass](recordBuilder.build()) should beRight(r)
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
        Decoder.decode[RecordWithListOfOptionalCaseClass](recordBuilder.build()) should beRight(r)
      }
    }

    "decode a list of map" in {
      forAll { writerRecord: WriterRecordWithListOfMap =>
        val writerSchema = AvroSchema[WriterRecordWithListOfMap].schema.value

        val javaList = writerRecord.field1.map(_.asJava).asJava

        val record        = new GenericData.Record(writerSchema)
        val recordBuilder = new GenericRecordBuilder(record)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", javaList)

        val expected = ReaderRecordWithListOfMap(writerRecord.field1)
        Decoder.decode[ReaderRecordWithListOfMap](recordBuilder.build()) should beRight(expected)
      }
    }

    "decode a list of enum" in {
      forAll { writerRecord: WriterRecordWithListOfEnum =>
        val writerSchema = AvroSchema[WriterRecordWithListOfEnum].schema.value

        val javaList = writerRecord.field1.map(_.toString).asJava

        val record        = new GenericData.Record(writerSchema)
        val recordBuilder = new GenericRecordBuilder(record)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", javaList)

        val expected = ReaderRecordWithListOfEnum(writerRecord.field1)
        Decoder.decode[ReaderRecordWithListOfEnum](recordBuilder.build()) should beRight(expected)
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

  case class WriterRecordWithListOfMap(writerField: String, field1: List[Map[String, Boolean]])

  case class ReaderRecordWithListOfMap(field1: List[Map[String, Boolean]])

}

private[this] object ArraySpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithListOfEnum(field1: List[A], writerField: Int)

  case class ReaderRecordWithListOfEnum(field1: List[A])

}
