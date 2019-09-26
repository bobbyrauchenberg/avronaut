package unit.decoder

import cats.syntax.either._

import collection.JavaConverters._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.decoder.Decoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.decoder.utils.RunAssert._

class EitherUnionSpec extends UnitSpecBase {

  "decoder" should {
    "decode a union of either A or B" in {
      forAll { u: Union =>
        val expected = u.field.fold(identity, identity)
        runAssert(expected, u)
      }
    }

    "decode a union of case classes" in {
      forAll { (u: WriterUnionWithCaseClass, toIgnore: Cupcat) =>
        val outerSchema  = AvroSchema[WriterUnionWithCaseClass].schema.value
        val cupcatSchema = AvroSchema[Cupcat].schema.value
        val rendalSchema = AvroSchema[Rendal].schema.value

        val outerRecord  = new GenericData.Record(outerSchema)
        val cupcatRecord = new GenericData.Record(cupcatSchema)
        val rendalRecord = new GenericData.Record(rendalSchema)

        val readerSchema = AvroSchema[ReaderUnionWithCaseClass].schema.value

        val recordBuilder = new GenericRecordBuilder(outerRecord)
        u.field match {
          case Right(rendal) =>
            rendalRecord.put(0, rendal.field1)
            rendalRecord.put(1, rendal.field2)
            recordBuilder.set("field", rendalRecord)
          case Left(cupcat) =>
            cupcatRecord.put(0, cupcat.field1)
            cupcatRecord.put(1, cupcat.field2)
            recordBuilder.set("field", cupcatRecord)
        }
        recordBuilder.set("fieldReaderIgnores", toIgnore)
        Decoder.decode[ReaderUnionWithCaseClass](readerSchema, recordBuilder.build()) should beRight(
          ReaderUnionWithCaseClass(u.field))
      }
    }

    "decode a case class with an optional either" in {
      forAll { u: UnionWithOptionalEither =>
        val outerSchema  = AvroSchema[UnionWithOptionalEither].schema.value
        val cupcatSchema = AvroSchema[Cupcat].schema.value
        val rendalSchema = AvroSchema[Rendal].schema.value

        val outerRecord  = new GenericData.Record(outerSchema)
        val cupcatRecord = new GenericData.Record(cupcatSchema)
        val rendalRecord = new GenericData.Record(rendalSchema)

        val recordBuilder = new GenericRecordBuilder(outerRecord)
        u.field match {
          case Some(Right(rendal)) =>
            rendalRecord.put(0, rendal.field1)
            rendalRecord.put(1, rendal.field2)
            recordBuilder.set("field", rendalRecord)
          case Some(Left(cupcat)) =>
            cupcatRecord.put(0, cupcat.field1)
            cupcatRecord.put(1, cupcat.field2)
            recordBuilder.set("field", cupcatRecord)
          case None => outerRecord.put(0, null)
        }

        Decoder.decode[UnionWithOptionalEither](outerSchema, recordBuilder.build()) should beRight(u)

      }
    }

    "decode a case class with an either with an optional field" in {
      forAll { u: UnionWithEitherOfOption =>
        val outerSchema  = AvroSchema[UnionWithEitherOfOption].schema.value
        val cupcatSchema = AvroSchema[Cupcat].schema.value
        val rendalSchema = AvroSchema[Rendal].schema.value

        val outerRecord  = new GenericData.Record(outerSchema)
        val cupcatRecord = new GenericData.Record(cupcatSchema)
        val rendalRecord = new GenericData.Record(rendalSchema)

        val recordBuilder = new GenericRecordBuilder(outerRecord)
        u.field match {
          case Right(Left(rendal)) =>
            rendalRecord.put(0, rendal.field1)
            rendalRecord.put(1, rendal.field2)
            recordBuilder.set("field", rendalRecord)
          case Right(Right(str)) =>
            recordBuilder.set("field", str)
          case Left(Some(cupcat)) =>
            cupcatRecord.put(0, cupcat.field1)
            cupcatRecord.put(1, cupcat.field2)
            recordBuilder.set("field", cupcatRecord)
          case Left(None) =>
            outerRecord.put(0, null)
        }
        Decoder.decode[UnionWithEitherOfOption](outerSchema, recordBuilder.build()) should beRight(u)
      }
    }

    "decode a case class with an either with an optional list of record" in {
      forAll { field: Either[Option[List[Cupcat]], Either[Rendal, String]] =>
        val u = UnionWithEitherOfList(field)

        val outerSchema  = AvroSchema[UnionWithEitherOfList].schema.value
        val cupcatSchema = AvroSchema[Cupcat].schema.value
        val rendalSchema = AvroSchema[Rendal].schema.value

        val outerRecord  = new GenericData.Record(outerSchema)
        val rendalRecord = new GenericData.Record(rendalSchema)

        val recordBuilder = new GenericRecordBuilder(outerRecord)
        u.field match {
          case Right(Left(rendal)) =>
            rendalRecord.put(0, rendal.field1)
            rendalRecord.put(1, rendal.field2)
            recordBuilder.set("field", rendalRecord)
          case Right(Right(str)) =>
            recordBuilder.set("field", str)
          case Left(Some(l)) =>
            val buildRecords = l.map { cupcat =>
              val cupcatRecord = new GenericData.Record(cupcatSchema)
              cupcatRecord.put(0, cupcat.field1)
              cupcatRecord.put(1, cupcat.field2)
              cupcatRecord
            }
            recordBuilder.set("field", buildRecords.asJava)
          case Left(None) =>
            outerRecord.put(0, null)
        }

        Decoder.decode[UnionWithEitherOfList](outerSchema, recordBuilder.build()) should beRight(u)
      }
    }

    "decode a caseclass with a default either value" in {
      val outerSchema   = AvroSchema[UnionWithDefaultCaseClass].schema.value
      val outerRecord   = new GenericData.Record(outerSchema)
      val recordBuilder = new GenericRecordBuilder(outerRecord)
      recordBuilder.set("field", "232")

      val expected = UnionWithDefaultCaseClass()
      Decoder.decode[UnionWithDefaultCaseClass](outerSchema, recordBuilder.build()) should beRight(expected)
    }

    "decode a union of null and enum" in {
      import EitherUnionSpec._
      forAll { record: WriterRecordWithEnum =>
        val writerSchema = AvroSchema[WriterRecordWithEnum].schema.value
        val readerSchema = AvroSchema[ReaderRecordWithEnum].schema.value

        val builder = new GenericRecordBuilder(new GenericData.Record(writerSchema))

        record.field1 match {
          case Left(enum)     => builder.set("field1", enum.toString)
          case Right(boolean) => builder.set("field1", boolean)
        }
        builder.set("writerField", record.writerField)
        builder.set("field2", record.field2)

        val expected = ReaderRecordWithEnum(record.field2, record.field1)

        Decoder.decode[ReaderRecordWithEnum](readerSchema, builder.build()) should beRight(expected)
      }
    }

  }

  case class Union(field: Either[Boolean, Int])

  case class Cupcat(field1: Boolean, field2: Float)
  case class Rendal(field1: Boolean, field2: String)
  case class WriterUnionWithCaseClass(fieldReaderIgnores: Cupcat, field: Either[Cupcat, Rendal])
  case class ReaderUnionWithCaseClass(field: Either[Cupcat, Rendal])

  case class UnionWithDefaultCaseClass(field: Either[Cupcat, Rendal] = Cupcat(true, 123.8f).asLeft)

  case class UnionWithOptionalEither(field: Option[Either[Cupcat, Rendal]])
  case class UnionWithEitherOfOption(field: Either[Option[Cupcat], Either[Rendal, String]])
  case class UnionWithEitherOfList(field: Either[Option[List[Cupcat]], Either[Rendal, String]])
}

private[this] object EitherUnionSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithEnum(field1: Either[A, Boolean], writerField: String, field2: Boolean)
  case class ReaderRecordWithEnum(field2: Boolean, field1: Either[A, Boolean])
}
