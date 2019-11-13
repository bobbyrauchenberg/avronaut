package unit.encoder

import cats.syntax.either._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.avronaut.Codec
import com.rauchenberg.avronaut.Codec._
import com.rauchenberg.avronaut.schema.AvroSchema
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import unit.encoder.EitherUnionSpec.WriterRecordWithEnum
import unit.utils.UnitSpecBase

import scala.collection.JavaConverters._

class EitherUnionSpec extends UnitSpecBase {

  "encoder" should {
    "encode a union of either A or B" in new TestContext {
      forAll { u: Union =>
        val schema   = Codec.schema[Union].value
        val expected = new GenericData.Record(schema)
        u.field.fold(v => expected.put("field", v), v => expected.put("field", v))

        u.encode should beRight(expected.asInstanceOf[GenericRecord])
      }
    }

    "encode a union of case classes" in new TestContext {
      forAll { u: WriterUnionWithCaseClass =>
        val schema       = Codec.schema[WriterUnionWithCaseClass].value
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(schema)
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

        u.encode
          .map(_.asInstanceOf[GenericData.Record]) should beRight(recordBuilder.build)

      }
    }

    "encode a case class with an optional either" in new TestContext {
      forAll { u: UnionWithOptionalEither =>
        val schema       = Codec.schema[UnionWithOptionalEither].value
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(schema)
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

        u.encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])

      }
    }

    "encode a case class with an either with an optional field" in new TestContext {
      forAll { u: UnionWithEitherOfOption =>
        val schema       = Codec.schema[UnionWithEitherOfOption].value
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(schema)
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
        u.encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a case class with an either with an optional list of record" in new TestContext {
      forAll { field: Either[Option[List[Cupcat]], Either[Rendal, String]] =>
        val u            = UnionWithEitherOfList(field)
        val schema       = Codec.schema[UnionWithEitherOfList].value
        val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value
        val rendalSchema = AvroSchema.toSchema[Rendal].data.value

        val outerRecord  = new GenericData.Record(schema)
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

        u.encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
      }
    }

    "encode a caseclass with a default either value" in new TestContext {

      val schema       = Codec.schema[UnionWithDefaultCaseClass].value
      val cupcatSchema = AvroSchema.toSchema[Cupcat].data.value.schema
      val outerRecord  = new GenericData.Record(schema)
      val cupcatRecord = new GenericData.Record(cupcatSchema)
      cupcatRecord.put(0, true)
      cupcatRecord.put(1, 123.8f)

      val recordBuilder = new GenericRecordBuilder(outerRecord)
      recordBuilder.set("field", cupcatRecord)

      UnionWithDefaultCaseClass().encode should beRight(recordBuilder.build.asInstanceOf[GenericRecord])
    }

    "encode a union of sealed trait and boolean" in new TestContext {
      import EitherUnionSpec._
      forAll { record: WriterRecordWithEnum =>
        val schema  = Codec.schema[WriterRecordWithEnum].value
        val builder = new GenericRecordBuilder(new GenericData.Record(schema))

        record.field1 match {
          case Left(enum) =>
            val enumSchema = schema.getFields.asScala.head.schema().getTypes.asScala.head
            builder.set("field1", GenericData.get.createEnum(enum.toString, enumSchema))
          case Right(boolean) => builder.set("field1", boolean)
        }
        builder.set("writerField", record.writerField)
        builder.set("field2", record.field2)

        record.encode should beRight(builder.build.asInstanceOf[GenericRecord])
      }
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
    implicit val unionCodec: Codec[Union]                                         = Codec[Union]
    implicit val writerUnionWithCaseClassEncoder: Codec[WriterUnionWithCaseClass] = Codec[WriterUnionWithCaseClass]
    implicit val unionWithOptionalEitherCodec: Codec[UnionWithOptionalEither]     = Codec[UnionWithOptionalEither]
    implicit val unionWithEitherOfOptionCodec: Codec[UnionWithEitherOfOption]     = Codec[UnionWithEitherOfOption]
    implicit val unionWithEitherOfListCodec: Codec[UnionWithEitherOfList]         = Codec[UnionWithEitherOfList]
    implicit val unionWithDefaultCaseClassCodec: Codec[UnionWithDefaultCaseClass] = Codec[UnionWithDefaultCaseClass]
    implicit val writerRecordWithEnumCodec: Codec[WriterRecordWithEnum]           = Codec[WriterRecordWithEnum]
  }

}

object EitherUnionSpec {

  sealed trait A
  case object B extends A
  case object C extends A

  case class WriterRecordWithEnum(field1: Either[A, Boolean], writerField: String, field2: Boolean)
}
