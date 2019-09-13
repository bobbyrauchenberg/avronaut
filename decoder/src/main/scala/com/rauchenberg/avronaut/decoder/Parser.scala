package com.rauchenberg.avronaut.decoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common.{AvroEnum, AvroField, AvroRecord, AvroType, AvroUnion, Error, ParseFail, Result}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.SeqWrapper

object Parser {

  implicit val formats = DefaultFormats

  def decode[A](readerSchema: Schema, genericRecord: GenericData.Record)(implicit decoder: Decoder[A]) =
    parse(readerSchema, genericRecord).flatMap(decoder.apply(_))

  def decodeToJson[A](readerSchema: Schema, genericRecord: GenericData.Record)(implicit decoder: Decoder[A]) =
    decode[A](readerSchema, genericRecord).map(write(_))

  def parse[A](readerSchema: Schema, genericRecord: GenericData.Record): Result[AvroType] = {

    def recurse(schema: Schema, genRec: GenericData.Record): Result[List[AvroType]] =
      indexedFieldsFor(schema)
        .foldLeft(List.empty[Result[AvroType]]) {
          case (acc, (field, _)) =>
            val value       = genRec.get(field.name) //needs to be null checked / Option(_) - java :(
            val fieldName   = s"${schema.getFullName}.${field.name}"
            val errorValues = "'$fieldName', '$value', '${genericRecord.getSchema}'"

            acc :+ (field.schema().getType match {
              case Schema.Type.RECORD =>
                value match {
                  case gr: GenericData.Record => recurse(field.schema(), gr).map(AvroRecord(fieldName, _))
                  case _ =>
                    ParseFail(fieldName, s"expected a GenericRecord for $errorValues").asRight
                }
              case Schema.Type.UNION =>
                parseUnion(genRec, field.name, field.schema(), value).map(AvroUnion(fieldName, _))
              case Schema.Type.ARRAY =>
                parseArray(genRec, fieldName, field.schema, value)
                  .fold(_ => ParseFail(fieldName, s"non-list when parsing array for $errorValues").asRight, _.asRight)
              case Schema.Type.ENUM =>
                AvroEnum(fieldName, value).asRight
              case _ => avroToAST(field.schema.getType, value).map(AvroField(fieldName, _))
            })
        }
        .sequence
    recurse(readerSchema, genericRecord).map(AvroRecord(s"${genericRecord.getSchema.getFullName}", _))
  }

  def parseArray[A](genRec: GenericData.Record, fieldName: String, schema: Schema, value: A): Result[AvroType] = {

    val asList = value.asInstanceOf[SeqWrapper[A]].asScala.toVector

    def dispatch(f: => A => Result[AvroType]) = asList.traverse(f).flatMap(toAvroArray(fieldName, _))

    schema.getElementType.getType match {
      case Schema.Type.RECORD =>
        dispatch(parseRecord(schema.getElementType, fieldName, _))
      case Schema.Type.UNION =>
        dispatch(parseUnion(genRec, fieldName, schema.getElementType, _).flatMap(toAvroUnion(fieldName, _)))
      case _ => dispatch(avroToAST(schema.getElementType.getType, _))
    }

  }

  def parseRecord[A](schema: Schema, fieldName: String, value: A): Result[AvroType] =
    value match {
      case gr: GenericData.Record => parse(schema, gr)
      case _                      => ParseFail(fieldName, "should have been a record").asRight
    }

  def parseUnion[A](genericRecord: GenericData.Record,
                    fieldName: String,
                    schema: Schema,
                    value: A): Result[AvroType] = {

    val failureMsg = s"couldn't parse union for '$fieldName', '$value', '$schema'"

    @tailrec
    def loop(schemas: List[Schema]): Result[AvroType] = schemas match {

      case Nil => ParseFail(fieldName, failureMsg).asRight
      case h :: t =>
        val schemaType = h.getType

        (schemaType match {
          case Schema.Type.NULL => toAvroNull(value)
          case Schema.Type.RECORD =>
            schema.getTypes.asScala
              .filter(_.getType == Schema.Type.RECORD)
              .toVector
              .foldLeft(Error("wasn't able to parse a union").asLeft[AvroType]) {
                case (acc, recordSchema) =>
                  if (acc.isRight) acc
                  else
                    parseRecord(recordSchema, recordSchema.getFullName, value) match {
                      case r @ Right(AvroRecord(_, _)) => r
                      case _                           => acc
                    }
              }
          case Schema.Type.ARRAY =>
            if (value.isInstanceOf[SeqWrapper[_]]) parseArray(genericRecord, fieldName, h, value)
            else ParseFail(fieldName, failureMsg).asRight
          case _ => avroToAST(schemaType, value)
        }) match {
          case Right(ParseFail(_, _)) => loop(t)
          case Right(v)               => v.asRight
          case _                      => loop(t)
        }
    }

    loop(schema.getTypes.asScala.toList)
  }

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

  private def indexedFieldsFor(s: Schema) = s.getFields.asScala.toVector.zipWithIndex

}
