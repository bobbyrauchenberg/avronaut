package unit.encoder

import cats.syntax.either._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecordBuilder}
import unit.utils.UnitSpecBase
import RunRoundTripAssert._

import scala.collection.JavaConverters._

class EitherUnionSpec extends UnitSpecBase {

  "encoder" should {
    "encode a union of either A or B" in {
      forAll { u: Union =>
        val schema           = AvroSchema.toSchema[Union].value
        implicit val encoder = Encoder[Union]

        val expected = new GenericData.Record(schema.schema)
        u.field.fold(v => expected.put("field", v), v => expected.put("field", v))

        Encoder.encode[Union](u, schema) should beRight(expected)
      }
    }

    "encode a union of case classes" in {
      forAll { u: WriterUnionWithCaseClass =>
        val schema           = AvroSchema.toSchema[WriterUnionWithCaseClass].value
        implicit val encoder = Encoder[WriterUnionWithCaseClass]

        val cupcatSchema = AvroSchema.toSchema[Cupcat].value
        val rendalSchema = AvroSchema.toSchema[Rendal].value

        val outerRecord  = new GenericData.Record(schema.schema)
        val cupcatRecord = new GenericData.Record(cupcatSchema.schema)
        val rendalRecord = new GenericData.Record(rendalSchema.schema)

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

        Encoder.encode[WriterUnionWithCaseClass](u, schema)

      }
    }

    "encode a case class with an optional either" in {
      forAll { u: UnionWithOptionalEither =>
        val schema           = AvroSchema.toSchema[UnionWithOptionalEither].value
        implicit val encoder = Encoder[UnionWithOptionalEither]

        val cupcatSchema = AvroSchema.toSchema[Cupcat].value
        val rendalSchema = AvroSchema.toSchema[Rendal].value

        val outerRecord  = new GenericData.Record(schema.schema)
        val cupcatRecord = new GenericData.Record(cupcatSchema.schema)
        val rendalRecord = new GenericData.Record(rendalSchema.schema)

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

        Encoder.encode[UnionWithOptionalEither](u, schema)

      }
    }

    "encode a case class with an either with an optional field" in {
      forAll { u: UnionWithEitherOfOption =>
        val schema           = AvroSchema.toSchema[UnionWithEitherOfOption].value
        implicit val encoder = Encoder[UnionWithEitherOfOption]

        val cupcatSchema = AvroSchema.toSchema[Cupcat].value
        val rendalSchema = AvroSchema.toSchema[Rendal].value

        val outerRecord  = new GenericData.Record(schema.schema)
        val cupcatRecord = new GenericData.Record(cupcatSchema.schema)
        val rendalRecord = new GenericData.Record(rendalSchema.schema)

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
        Encoder.encode(u, schema) should beRight(recordBuilder.build)
      }
    }

    "encode a case class with an either with an optional list of record" in {
      forAll { field: Either[Option[List[Cupcat]], Either[Rendal, String]] =>
        val u                = UnionWithEitherOfList(field)
        implicit val encoder = Encoder[UnionWithEitherOfList]

        val schema       = AvroSchema.toSchema[UnionWithEitherOfList].value
        val cupcatSchema = AvroSchema.toSchema[Cupcat].value
        val rendalSchema = AvroSchema.toSchema[Rendal].value

        val outerRecord  = new GenericData.Record(schema.schema)
        val rendalRecord = new GenericData.Record(rendalSchema.schema)

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
              val cupcatRecord = new GenericData.Record(cupcatSchema.schema)
              cupcatRecord.put(0, cupcat.field1)
              cupcatRecord.put(1, cupcat.field2)
              cupcatRecord
            }
            recordBuilder.set("field", buildRecords.asJava)
          case Left(None) =>
            outerRecord.put(0, null)
        }

        Encoder.encode[UnionWithEitherOfList](u, schema) should beRight(recordBuilder.build)
      }
    }

    "encode a caseclass with a default either value" in {
      val schema = AvroSchema.toSchema[UnionWithDefaultCaseClass].value

      val cupcatSchema = AvroSchema.toSchema[Cupcat].value.schema

      implicit val encoder = Encoder[UnionWithDefaultCaseClass]
      val outerRecord      = new GenericData.Record(schema.schema)
      val cupcatRecord     = new GenericData.Record(cupcatSchema)
      cupcatRecord.put(0, true)
      cupcatRecord.put(1, 123.8f)

      val recordBuilder = new GenericRecordBuilder(outerRecord)
      recordBuilder.set("field", cupcatRecord)

      Encoder.encode(UnionWithDefaultCaseClass(), schema) should beRight(recordBuilder.build)
    }

    "encode a union of null and enum" in {
      import EitherUnionSpec._
      forAll { record: WriterRecordWithEnum =>
        val schema           = AvroSchema.toSchema[WriterRecordWithEnum].value
        implicit val encoder = Encoder[WriterRecordWithEnum]

        val builder = new GenericRecordBuilder(new GenericData.Record(schema.schema))

        record.field1 match {
          case Left(enum)     => builder.set("field1", enum.toString)
          case Right(boolean) => builder.set("field1", boolean)
        }
        builder.set("writerField", record.writerField)
        builder.set("field2", record.field2)

        Encoder.encode[WriterRecordWithEnum](record, schema) should beRight(builder.build)
      }
    }

    "roundtrip tests" in {
      runRoundTrip[Union]
      runRoundTrip[WriterUnionWithCaseClass]
      runRoundTrip[UnionWithOptionalEither]
      runRoundTrip[UnionWithEitherOfOption]
      runRoundTrip[UnionWithDefaultCaseClass]
    }

  }

  case class Union(field: Either[Boolean, Int])

  case class Cupcat(field1: Boolean, field2: Float)
  case class Rendal(field1: Boolean, field2: String)
  case class WriterUnionWithCaseClass(fieldReaderIgnores: Cupcat, field: Either[Cupcat, Rendal])

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
}
