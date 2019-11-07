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
        implicit val decoder = Decoder[RecordWithList]
        runListAssert(record.field, record)
      }
    }
    "decode a record with a seq" in {
      forAll { record: RecordWithSeq =>
        implicit val decoder = Decoder[RecordWithSeq]
        runListAssert(record.field, record)
      }
    }
    "decode a record with a vector" in {
      forAll { record: RecordWithSeq =>
        implicit val decoder = Decoder[RecordWithSeq]
        runListAssert(record.field, record)
      }
    }
    "decode a record with a list and a default value" in {
      val record           = RecordWithListDefault()
      implicit val decoder = Decoder[RecordWithListDefault]
      runListAssert(record.field, record)
    }
    "decode a record with a seq and a default value" in {
      val record           = RecordWithSeqDefault()
      implicit val decoder = Decoder[RecordWithSeqDefault]
      runListAssert(record.field, record)
    }
    "decode a record with a vector and a default value" in {
      val record           = RecordWithVectorDefault()
      implicit val decoder = Decoder[RecordWithVectorDefault]
      runListAssert(record.field, record)
    }

    "decode a record with a list of simple caseclass" in {
      forAll { record: RecordWithListOfSimpleCaseClass =>
        implicit val rootSchema = AvroSchema.toSchema[RecordWithListOfSimpleCaseClass].data
        val innerSchema         = AvroSchema.toSchema[InnerNested].data.value
        val decoder             = Decoder[RecordWithListOfSimpleCaseClass]

        val rootRecord    = new GenericData.Record(rootSchema.value.schema)
        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val innerRecords = record.field.map { innerNested =>
          val innerRecord = new GenericData.Record(innerSchema.schema)
          innerRecord.put(0, innerNested.field1)
          innerRecord.put(1, innerNested.field2)
          innerRecord
        }
        recordBuilder.set("field", innerRecords.asJava)

        Decoder.decode[RecordWithListOfSimpleCaseClass](recordBuilder.build, decoder) should beRight(record)
      }

    }

    "decode a record with a list of caseclass" in {
      forAll { r: RecordWithListOfCaseClass =>
        implicit val rootSchema = AvroSchema.toSchema[RecordWithListOfCaseClass].data
        val decoder             = Decoder[RecordWithListOfCaseClass]
        val outerSchema         = AvroSchema.toSchema[Nested].data.value
        val innerSchema         = AvroSchema.toSchema[InnerNested].data.value

        val rootRecord = new GenericData.Record(rootSchema.value.schema)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.zipWithIndex.map {
          case (outer, _) =>
            val outerRecord = new GenericData.Record(outerSchema.schema)
            val innerRecord = new GenericData.Record(innerSchema.schema)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Decoder.decode[RecordWithListOfCaseClass](recordBuilder.build, decoder) should beRight(r)
      }
    }

    "decode a record with a seq of caseclass" in {
      forAll { r: RecordWithSeqOfCaseClass =>
        val rootSchema  = AvroSchema.toSchema[RecordWithSeqOfCaseClass].data
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value

        val decoder = Decoder[RecordWithSeqOfCaseClass]

        val rootRecord = new GenericData.Record(rootSchema.value.schema)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            val outerRecord = new GenericData.Record(outerSchema.schema)
            val innerRecord = new GenericData.Record(innerSchema.schema)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Decoder.decode[RecordWithSeqOfCaseClass](recordBuilder.build, decoder) should beRight(r)
      }
    }
    "decode a record with a vector of caseclass" in {
      forAll { r: RecordWithVectorOfCaseClass =>
        val rootSchema  = AvroSchema.toSchema[RecordWithVectorOfCaseClass].data
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value
        val decoder     = Decoder[RecordWithVectorOfCaseClass]

        val rootRecord = new GenericData.Record(rootSchema.value.schema)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            val outerRecord = new GenericData.Record(outerSchema.schema)
            val innerRecord = new GenericData.Record(innerSchema.schema)
            innerRecord.put(0, outer.field2.field1)
            innerRecord.put(1, outer.field2.field2)

            outerRecord.put(0, outer.field1)
            outerRecord.put(1, innerRecord)
            outerRecord.put(2, outer.field3)
            outerRecord
        }.asJava
        recordBuilder.set("field", recordList)
        Decoder.decode[RecordWithVectorOfCaseClass](recordBuilder.build, decoder) should beRight(r)
      }
    }

    "decode a record with a list of optional caseclass" in {
      forAll { r: RecordWithListOfOptionalCaseClass =>
        val rootSchema  = AvroSchema.toSchema[RecordWithListOfOptionalCaseClass].data
        val outerSchema = AvroSchema.toSchema[Nested].data.value
        val innerSchema = AvroSchema.toSchema[InnerNested].data.value
        val decoder     = Decoder[RecordWithListOfOptionalCaseClass]

        val rootRecord = new GenericData.Record(rootSchema.value.schema)

        val recordBuilder = new GenericRecordBuilder(rootRecord)

        val recordList = r.field.map {
          case outer =>
            outer match {
              case None =>
                null
              case Some(v) =>
                val outerRecord = new GenericData.Record(outerSchema.schema)
                val innerRecord = new GenericData.Record(innerSchema.schema)
                innerRecord.put(0, v.field2.field1)
                innerRecord.put(1, v.field2.field2)

                outerRecord.put(0, v.field1)
                outerRecord.put(1, innerRecord)
                outerRecord.put(2, v.field3)
                outerRecord
            }

        }.asJava
        recordBuilder.set("field", recordList)
        Decoder.decode[RecordWithListOfOptionalCaseClass](recordBuilder.build, decoder) should beRight(r)
      }
    }

    "decode a list of map" in {
      forAll { writerRecord: WriterRecordWithListOfMap =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithListOfMap].data.value
        val decoder      = Decoder[ReaderRecordWithListOfMap]
        val javaList     = writerRecord.field1.map(_.asJava).asJava

        val record        = new GenericData.Record(writerSchema.schema)
        val recordBuilder = new GenericRecordBuilder(record)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", javaList)

        val expected = ReaderRecordWithListOfMap(writerRecord.field1)
        Decoder.decode[ReaderRecordWithListOfMap](recordBuilder.build, decoder) should beRight(expected)
      }
    }

    "decode a list of enum" in {
      forAll { writerRecord: WriterRecordWithListOfEnum =>
        val writerSchema = AvroSchema.toSchema[WriterRecordWithListOfEnum].data.value
        val decoder      = Decoder[ReaderRecordWithListOfEnum]

        val javaList = writerRecord.field1.map(_.toString).asJava

        val record        = new GenericData.Record(writerSchema.schema)
        val recordBuilder = new GenericRecordBuilder(record)

        recordBuilder.set("writerField", writerRecord.writerField)
        recordBuilder.set("field1", javaList)

        val expected = ReaderRecordWithListOfEnum(writerRecord.field1)
        Decoder.decode[ReaderRecordWithListOfEnum](recordBuilder.build, decoder) should beRight(expected)
      }
    }

  }

  case class RecordWithList(field: List[String])
  implicit val recordWithListSchema: AvroSchema[RecordWithList] = AvroSchema.toSchema[RecordWithList]

  case class RecordWithSeq(field: Seq[String])
  implicit val recordWithSeq: AvroSchema[RecordWithSeq] = AvroSchema.toSchema[RecordWithSeq]

  case class RecordWithVector(field: Vector[String])
  implicit val recordWithVector: AvroSchema[RecordWithVector] = AvroSchema.toSchema[RecordWithVector]

  case class RecordWithListDefault(field: List[String] = List("cup", "cat"))
  implicit val recordWithListDefault: AvroSchema[RecordWithListDefault] = AvroSchema.toSchema[RecordWithListDefault]

  case class RecordWithSeqDefault(field: Seq[String] = Seq("cup", "cat"))
  implicit val recordWithSeqDefault: AvroSchema[RecordWithSeqDefault] = AvroSchema.toSchema[RecordWithSeqDefault]

  case class RecordWithVectorDefault(field: Vector[String] = Vector("cup", "cat"))
  implicit val recordWithVectorDefault: AvroSchema[RecordWithVectorDefault] =
    AvroSchema.toSchema[RecordWithVectorDefault]

  case class InnerNested(field1: String, field2: Int)
  implicit val recordInnerNested: AvroSchema[InnerNested] = AvroSchema.toSchema[InnerNested]

  case class Nested(field1: String, field2: InnerNested, field3: Int)
  implicit val recordNested: AvroSchema[Nested] = AvroSchema.toSchema[Nested]

  case class RecordWithListOfSimpleCaseClass(field: List[InnerNested])
  implicit val recordWithListOfSimpleCaseClass: AvroSchema[RecordWithListOfSimpleCaseClass] =
    AvroSchema.toSchema[RecordWithListOfSimpleCaseClass]

  case class RecordWithListOfCaseClass(field: List[Nested])
  implicit val recordWithListOfCaseClass: AvroSchema[RecordWithListOfCaseClass] =
    AvroSchema.toSchema[RecordWithListOfCaseClass]

  case class RecordWithSeqOfCaseClass(field: Seq[Nested])
  implicit val recordWithSeqOfCaseClass: AvroSchema[RecordWithSeqOfCaseClass] =
    AvroSchema.toSchema[RecordWithSeqOfCaseClass]

  case class RecordWithVectorOfCaseClass(field: Vector[Nested])
  implicit val recordWithVectorOfCaseClass: AvroSchema[RecordWithVectorOfCaseClass] =
    AvroSchema.toSchema[RecordWithVectorOfCaseClass]

  case class RecordWithListOfOptionalCaseClass(field: List[Option[Nested]])
  implicit val recordWithListOfOptionalCaseClass: AvroSchema[RecordWithListOfOptionalCaseClass] =
    AvroSchema.toSchema[RecordWithListOfOptionalCaseClass]

  case class WriterRecordWithListOfMap(writerField: String, field1: List[Map[String, Boolean]])
  implicit val writerRecordWithListOfMap: AvroSchema[WriterRecordWithListOfMap] =
    AvroSchema.toSchema[WriterRecordWithListOfMap]

  case class ReaderRecordWithListOfMap(field1: List[Map[String, Boolean]])
  implicit val readerRecordWithListOfMap: AvroSchema[ReaderRecordWithListOfMap] =
    AvroSchema.toSchema[ReaderRecordWithListOfMap]
}

private[this] object ArraySpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithListOfEnum(field1: List[A], writerField: Int)

  case class ReaderRecordWithListOfEnum(field1: List[A])

}
