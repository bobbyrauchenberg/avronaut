package com.rauchenberg.avronaut.decoder

import com.rauchenberg.avronaut.common.{safe, AvroRecord, AvroType, Error, ParseFail, Result}
import cats.implicits._
import com.rauchenberg.avronaut.common
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.Schema.Type._

import scala.collection.convert.Wrappers.SeqWrapper
import scala.collection.JavaConverters._
import AvroType._

object Parser2 {

  def decode[A](readerSchema: Schema, genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    parse(readerSchema, genericRecord).flatMap(decoder.apply(_))

  private def parse(schema: Schema, gr: GenericRecord): Result[AvroType] =
    fieldsFor(schema)
      .foldLeft(List.empty[Result[AvroType]]) {
        case (acc, field) =>
          val fieldSchema = field.schema()
          val fieldName   = s"${schema.getFullName}.${field.name}"

          acc :+ (fieldSchema.getType match {
            case RECORD =>
              parseRecord(fieldName, fieldSchema, gr)
            case ARRAY =>
              parseArray(fieldName, fieldSchema, gr)
            case UNION =>
              parseUnion(fieldName, fieldSchema, gr)
            case ENUM =>
              parseEnum(fieldName, fieldSchema, gr)
            case _ => avroToAST(schema.getType, gr.get(fieldSchema.getName))
          })
          acc
      }
      .sequence
      .map(AvroRecord("test", _))

  private def parseRecord(fieldName: String, schema: Schema, gr: GenericRecord): Result[AvroType] =
    gr.get(schema.getName) match {
      case gr: GenericRecord => parse(schema, gr)
      case _                 => ParseFail(fieldName, "should have been a record").asRight
    }

  private def parseArray[A](fieldName: String, schema: Schema, gr: GenericRecord): Result[AvroType] = {
    val res: Either[common.Error, Vector[AvroType]] = safe(gr.get(schema.getName).asInstanceOf[SeqWrapper[A]]).flatMap {
      sw =>
        val asList = sw.asScala.toVector
        (schema.getType match {
          case RECORD =>
            asList.map { value =>
              parseRecordField(fieldName, schema, value)
            }
          case UNION =>
            asList.map { value =>
              parseUnionField(fieldName, schema, value)
            }
          case ENUM =>
            asList.map { value =>
              parseEnumField(fieldName, schema, value)
            }
          case ARRAY =>
            asList.map { value =>
              parseArrayField(fieldName, schema, value)
            }
          case _ => Vector(Error("not implemented").asLeft)
        }).sequence
    }
    res.flatMap(toAvroArray(fieldName, _))
  }

  def parseRecordField[A](fieldName: String, schema: Schema, value: A) =
    value match {
      case gr: GenericData.Record => parse(schema, gr)
      case _                      => ParseFail(fieldName, "should have been a record").asRight
    }

  def parseUnionField[A](fieldName: String, schema: Schema, value: A) = ???

  def parseEnumField[A](fieldName: String, schema: Schema, value: A) = ???

  def parseArrayField[A](fieldName: String, schema: Schema, value: A) = ???

  def avroToAST[A](field: Schema.Type, value: A) = field match {
    case Schema.Type.STRING  => toAvroString(value)
    case Schema.Type.INT     => toAvroInt(value)
    case Schema.Type.LONG    => toAvroLong(value)
    case Schema.Type.FLOAT   => toAvroFloat(value)
    case Schema.Type.DOUBLE  => toAvroDouble(value)
    case Schema.Type.BOOLEAN => toAvroBool(value)
    case Schema.Type.BYTES   => toAvroBytes(value)
    case Schema.Type.NULL    => toAvroNull(value)
    case _                   => Error("boom").asLeft
  }

  private def parseUnion(fieldName: String, schema: Schema, gr: GenericRecord): Result[AvroType] = ???

  private def parseEnum(fieldName: String, schema: Schema, gr: GenericRecord): Result[AvroType] = ???

  private def fieldsFor(s: Schema) = s.getFields.asScala.toVector

}
