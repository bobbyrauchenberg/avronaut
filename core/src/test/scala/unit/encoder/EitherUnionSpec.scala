package unit.encoder

import cats.syntax.either._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.encoder.Encoder
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.encoder.EitherUnionSpec.WriterRecordWithEnum
import unit.encoder.RunRoundTripAssert._
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class EitherUnionSpec extends UnitSpecBase {

  "encoder" should {
    "encode a union of either A or B" in new TestContext {
      forAll { u: Union =>
        val expected = new GenericData.Record(unionSchema.data.value.schema)
        u.field.fold(v => expected.put("field", v), v => expected.put("field", v))

        Encoder.encode[Union](u, unionEncoder) should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a union of case classes" in new TestContext {
      forAll { u: WriterUnionWithCaseClass =>
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(writerUnionWithCaseClassSchema.data.value.schema)
        val cupcatRecord = new GenericData.Record(cupcatSchema.schema)
        val rendalRecord = new GenericData.Record(rendalSchema.schema)

        val recordBuilder = new GenericRecordBuilder(outerRecord)
        recordBuilder.set("field1", u.field1)

        u.field1 match {
          case Right(rendal) =>
            rendalRecord.put(0, rendal.field1)
            rendalRecord.put(1, rendal.field2)
            recordBuilder.set("field1", rendalRecord)
          case Left(cupcat) =>
            cupcatRecord.put(0, cupcat.field1)
            cupcatRecord.put(1, cupcat.field2)
            recordBuilder.set("field1", cupcatRecord)
        }

        (Encoder
          .encode[WriterUnionWithCaseClass](u, writerUnionWithCaseClassEncoder))
          .map(_.asInstanceOf[GenericData.Record]) should beRight(recordBuilder.build)

      }
    }

    "encode a case class with an optional either" in new TestContext {
      forAll { u: UnionWithOptionalEither =>
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(unionWithOptionalEitherSchema.data.value.schema)
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

        Encoder
          .encode[UnionWithOptionalEither](u, unionWithOptionalEitherEncoder) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])

      }
    }

    "encode a case class with an either with an optional field" in new TestContext {
      forAll { u: UnionWithEitherOfOption =>
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(unionWithEitherOfOptionSchema.data.value.schema)
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
        Encoder.encode(u, unionWithEitherOfOptionEncoder) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with an either with an optional list of record" in new TestContext {
      forAll { field: Either[Option[List[Cupcat]], Either[Rendal, String]] =>
        val u = UnionWithEitherOfList(field)

        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(unionWithEitherOfListSchema.data.value.schema)
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

        Encoder
          .encode[UnionWithEitherOfList](u, unionWithEitherOfListEncoder) should beRight(
          recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a caseclass with a default either value" in new TestContext {

      val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value.schema

      val outerRecord  = new GenericData.Record(unionWithDefaultCaseClassSchema.data.value.schema)
      val cupcatRecord = new GenericData.Record(cupcatSchema)
      cupcatRecord.put(0, true)
      cupcatRecord.put(1, 123.8f)

      val recordBuilder = new GenericRecordBuilder(outerRecord)
      recordBuilder.set("field", cupcatRecord)

      Encoder.encode(UnionWithDefaultCaseClass(), unionWithDefaultCaseClassEncoder) should beRight(
        recordBuilder.build.asInstanceOf[GenericRecord])
    }

    "encode a union of null and enum" in new TestContext {
      import EitherUnionSpec._
      forAll { record: WriterRecordWithEnum =>
        val builder = new GenericRecordBuilder(new GenericData.Record(writerRecordWithEnumSchema.data.value.schema))

        record.field1 match {
          case Left(enum)     => builder.set("field1", enum.toString)
          case Right(boolean) => builder.set("field1", boolean)
        }
        builder.set("writerField", record.writerField)
        builder.set("field2", record.field2)

        Encoder
          .encode[WriterRecordWithEnum](record, writerRecordWithEnumEncoder) should beRight(
          builder.build.asInstanceOf[GenericRecord])
      }
    }

    "do a roundtrip encode and decode" in new TestContext {
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
  case class WriterUnionWithCaseClass(field1: Either[Cupcat, Rendal])

  case class UnionWithDefaultCaseClass(field: Either[Cupcat, Rendal] = Cupcat(true, 123.8f).asLeft)

  case class UnionWithOptionalEither(field: Option[Either[Cupcat, Rendal]])
  case class UnionWithEitherOfOption(field: Either[Option[Cupcat], Either[Rendal, String]])
  case class UnionWithEitherOfList(field: Either[Option[List[Cupcat]], Either[Rendal, String]])

  trait TestContext {
    implicit val unionEncoder                   = Encoder[Union]
    implicit val unionSchema: AvroSchema[Union] = AvroSchema.toSchema[Union]

    implicit val writerUnionWithCaseClassEncoder = Encoder[WriterUnionWithCaseClass]
    implicit val writerUnionWithCaseClassSchema: AvroSchema[WriterUnionWithCaseClass] =
      AvroSchema.toSchema[WriterUnionWithCaseClass]

    implicit val unionWithOptionalEitherEncoder = Encoder[UnionWithOptionalEither]
    implicit val unionWithOptionalEitherSchema: AvroSchema[UnionWithOptionalEither] =
      AvroSchema.toSchema[UnionWithOptionalEither]

    implicit val unionWithEitherOfOptionEncoder = Encoder[UnionWithEitherOfOption]
    implicit val unionWithEitherOfOptionSchema: AvroSchema[UnionWithEitherOfOption] =
      AvroSchema.toSchema[UnionWithEitherOfOption]

    implicit val unionWithEitherOfListEncoder = Encoder[UnionWithEitherOfList]
    implicit val unionWithEitherOfListSchema: AvroSchema[UnionWithEitherOfList] =
      AvroSchema.toSchema[UnionWithEitherOfList]

    implicit val unionWithDefaultCaseClassEncoder = Encoder[UnionWithDefaultCaseClass]
    implicit val unionWithDefaultCaseClassSchema: AvroSchema[UnionWithDefaultCaseClass] =
      AvroSchema.toSchema[UnionWithDefaultCaseClass]

    implicit val writerRecordWithEnumEncoder = Encoder[WriterRecordWithEnum]
    implicit val writerRecordWithEnumSchema: AvroSchema[WriterRecordWithEnum] =
      AvroSchema.toSchema[WriterRecordWithEnum]
  }

}

object EitherUnionSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithEnum(field1: Either[A, Boolean], writerField: String, field2: Boolean)
}
