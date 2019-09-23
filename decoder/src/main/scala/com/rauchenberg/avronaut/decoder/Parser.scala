package com.rauchenberg.avronaut.decoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common.{
  safe,
  AvroEnum,
  AvroField,
  AvroRecord,
  AvroType,
  AvroUnion,
  Error,
  ParseFail,
  Result
}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.{LogicalType, Schema}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.SeqWrapper

object Parser {

  implicit val formats = DefaultFormats

  def decode[A](readerSchema: Schema, genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    parse(readerSchema, genericRecord).flatMap(decoder.apply(_))

  def decodeToJson[A](readerSchema: Schema, genericRecord: GenericRecord)(implicit decoder: Decoder[A]) =
    decode[A](readerSchema, genericRecord).map(write(_))

  def parse[A](readerSchema: Schema, genericRecord: GenericRecord): Result[AvroType] = {

    def recurse(schema: Schema, genRec: GenericRecord): Result[List[AvroType]] =
      indexedFieldsFor(schema)
        .foldLeft(List.empty[Result[AvroType]]) {
          case (acc, (field, _)) =>
            val value = genRec.get(field.name)

            val fieldName   = s"${schema.getFullName}.${field.name}"
            val errorValues = s"'$fieldName', '$value', '${genericRecord.getSchema}'"
            val schemaType  = field.schema.getType

            acc :+ (schemaType match {
              case Schema.Type.RECORD =>
                parseRecord(field.schema, fieldName, value)
              case Schema.Type.UNION =>
                parseUnion(genRec, field.name, field.schema, value).map(AvroUnion(fieldName, _))
              case Schema.Type.ARRAY =>
                parseArray(genRec, fieldName, field.schema, value)
                  .fold(_ => ParseFail(fieldName, s"couldn't parse array $errorValues").asRight, _.asRight)
              case Schema.Type.ENUM =>
                AvroEnum(fieldName, value).asRight
              case _ =>
                toAST(field.schema.getLogicalType, schemaType, fieldName, value, AvroField(fieldName, _))
            })

        }
        .sequence
    recurse(readerSchema, genericRecord).map(AvroRecord(s"${genericRecord.getSchema.getFullName}", _))
  }

  private def parseArray[A](genRec: GenericRecord, fieldName: String, schema: Schema, value: A): Result[AvroType] = {

    val failureMsg = s"parseArray can't cast '$value' to SeqWrapper for '$fieldName' and '$schema'"

    def dispatch(l: Vector[A], f: => A => Result[AvroType]) =
      l.traverse(f).flatMap(toAvroArray(fieldName, _))

    safe(value.asInstanceOf[SeqWrapper[A]].asScala.toVector).fold(
      _ => ParseFail(fieldName, failureMsg).asRight,
      asList =>
        schema.getElementType.getType match {
          case Schema.Type.RECORD =>
            dispatch(asList, parseRecord(schema.getElementType, fieldName, _))
          case Schema.Type.UNION =>
            dispatch(asList, parseUnion(genRec, fieldName, schema.getElementType, _).flatMap(toAvroUnion(fieldName, _)))
          case _ =>
            dispatch(asList, toAST(schema.getLogicalType, schema.getElementType.getType, fieldName, _))
      }
    )

  }

  private def parseUnion[A](genericRecord: GenericRecord,
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
            typesFor(schema)
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
            parseArray(genericRecord, fieldName, h, value)
          case _ =>
            toAST(schema.getLogicalType, schemaType, fieldName, value)
        }) match {
          case Right(ParseFail(_, _)) => loop(t)
          case Right(v)               => v.asRight
          case _                      => loop(t)
        }
    }

    loop(schema.getTypes.asScala.toList)
  }

  private def parseRecord[A](schema: Schema, fieldName: String, value: A): Result[AvroType] = {
    val failureMsg = s"expected a GenericRecord for '$fieldName', '$value', '$schema'"
    value match {
      case gr: GenericRecord => parse(schema, gr).map(v => AvroRecord(fieldName, List(v)))
      case _                 => ParseFail(fieldName, failureMsg).asRight
    }
  }

  private def toAST[A](logicalType: LogicalType,
                       schemaType: Schema.Type,
                       fieldName: String,
                       value: A,
                       transform: AvroType => AvroType = identity) =
    Option(logicalType).fold[Result[AvroType]](avroToAST(schemaType, value).map(transform)) { lt =>
      logicalAvroToAST(lt.getName, fieldName, value)
    }

  private def avroToAST[A](field: Schema.Type, value: A) = field match {
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

  private def logicalAvroToAST[A](logicalTypeName: String, fieldName: String, value: A): Result[AvroType] =
    logicalTypeName match {
      case "uuid" =>
        toAvroUUID(fieldName, value)
      case "timestamp-millis" => toAvroTimestamp(fieldName, value)
      case _                  => Error("logical type not supported").asLeft
    }

  private def typesFor(s: Schema) = s.getTypes.asScala

  private def indexedFieldsFor(s: Schema) = s.getFields.asScala.toVector.zipWithIndex

}
