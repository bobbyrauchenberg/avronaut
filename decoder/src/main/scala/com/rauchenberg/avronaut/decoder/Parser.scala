package com.rauchenberg.avronaut.decoder

import cats.implicits._
import collection.JavaConverters._
import scala.collection.convert.Wrappers.{MapWrapper, SeqWrapper}
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common._
import org.apache.avro.{LogicalType, Schema}
import org.apache.avro.Schema.Type.{
  ARRAY,
  BOOLEAN,
  BYTES,
  DOUBLE,
  ENUM,
  FLOAT,
  INT,
  LONG,
  MAP,
  NULL,
  RECORD,
  STRING,
  UNION
}
import org.apache.avro.generic.GenericRecord

import scala.annotation.tailrec

object Parser {

  def parse(toDecode: FullDecode) = {
    val schema = toDecode.schema
    val genRec = toDecode.genericRecord

    fieldsFor(schema).traverse { field =>
      val value       = genRec.get(field.name)
      val errorValues = s"'$value', '${schema}'"
      val schemaType  = field.schema.getType

      schemaType match {
        case RECORD =>
          parseRecord(field.schema, value)
        case UNION =>
          parseUnion(field.schema, value).map(AvroUnion(_))
        case ARRAY =>
          parseArray(field.schema, value)
            .fold(_ => AvroFail(s"couldn't parse array $errorValues").asRight, _.asRight)
        case ENUM =>
          AvroEnum(value).asRight
        case MAP =>
          parseMap(field.schema, value)
        case _ =>
          toAST(field.schema.getLogicalType, schemaType, value)
      }
    }
  }

  def parseMap[A](schema: Schema, value: A): Result[AvroType] = {

    val schemaType = schema.getValueType.getType
    val valueType  = schema.getValueType

    safe(
      value
        .asInstanceOf[MapWrapper[String, A]]
        .asScala
        .toMap[String, A]
        .toList
        .traverse {
          case (k, v) =>
            (schemaType match {
              case RECORD => parseRecord(valueType, v)
              case ARRAY  => parseArray(valueType, v)
              case UNION  => parseUnion(valueType, v)
              case _      => toAST(schema.getLogicalType, schemaType, v)
            }).map(AvroMapEntry(k, _))
        }
        .map(AvroMap(_))).flatten

  }

  private def parseArray[A](schema: Schema, value: A): Result[AvroType] = {

    val failureMsg = s"parseArray can't cast '$value' to SeqWrapper for '$schema'"

    val elementType = schema.getElementType

    safe(value.asInstanceOf[SeqWrapper[A]].asScala.toVector).fold(
      _ => Error(failureMsg).asLeft,
      _.traverse { value =>
        schema.getElementType.getType match {
          case RECORD =>
            parseRecord(elementType, value)
          case UNION =>
            parseUnion(elementType, value).flatMap(toAvroUnion(_))
          case MAP =>
            parseMap(elementType, value)
          case ENUM =>
            toAvroEnum(value)
          case _ =>
            toAST(schema.getLogicalType, elementType.getType, value)
        }
      }.flatMap(toAvroArray(_))
    )

  }

  private def parseUnion[A](schema: Schema, value: A): Result[AvroType] = {

    val failureMsg = s"couldn't parse union for '$value', '$schema'"

    @tailrec
    def loop(schemas: List[Schema]): Result[AvroType] = schemas match {

      case Nil => AvroFail(failureMsg).asRight
      case h :: t =>
        val schemaType = h.getType

        (schemaType match {
          case NULL => toAvroNull(value)
          case ARRAY =>
            parseArray(h, value)
          case ENUM =>
            toAvroEnum(value)
          case RECORD =>
            typesFor(schema)
              .filter(_.getType == RECORD)
              .foldLeft(Error("parsing union").asLeft[AvroType]) {
                case (acc, recordSchema) =>
                  if (acc.isRight) acc
                  else {
                    parseRecord(recordSchema, value) match {
                      case r @ Right(AvroRecord(list)) if (list.find(_.isFailure).isEmpty) => r
                      case _                                                               => acc
                    }
                  }
              }
          case _ =>
            toAST(schema.getLogicalType, schemaType, value)
        }) match {
          case Right(AvroFail(_)) => loop(t)
          case Right(v)           => v.asRight
          case _                  => loop(t)
        }
    }

    loop(enumToEndOfUnion(schema))
  }

  private def enumToEndOfUnion(schema: Schema) = {
    val schemaTypes = typeListFor(schema)
    schemaTypes.find(_.getType == ENUM).fold(schemaTypes) { enumSchema =>
      schemaTypes.filterNot(_.getType == ENUM) :+ enumSchema
    }
  }

  private def parseRecord[A](schema: Schema, value: A): Result[AvroType] = {
    val failureMsg = s"expected a GenericRecord for '$value', '$schema'"
    value match {
      case gr: GenericRecord => parse(FullDecode(schema, gr)).map(v => AvroRecord(v.toList))
      case _                 => AvroFail(failureMsg).asRight
    }
  }

  private def toAST[A](logicalType: LogicalType, schemaType: Schema.Type, value: A) =
    Option(logicalType).fold[Result[AvroType]](avroToAST(schemaType, value)) { lt =>
      logicalAvroToAST(lt.getName, value)
    }

  private def avroToAST[A](field: Schema.Type, value: A) =
    (field match {
      case STRING  => toAvroString(value)
      case INT     => toAvroInt(value)
      case LONG    => toAvroLong(value)
      case FLOAT   => toAvroFloat(value)
      case DOUBLE  => toAvroDouble(value)
      case BOOLEAN => toAvroBool(value)
      case BYTES   => toAvroBytes(value)
      case NULL    => toAvroNull(value)
      case _       => Error("boom").asLeft
    }).fold(e => AvroFail(e.msg).asRight, _.asRight)

  private def logicalAvroToAST[A](logicalTypeName: String, value: A): Result[AvroType] =
    (logicalTypeName match {
      case "uuid" =>
        toAvroUUID(value)
      case "timestamp-millis" => toAvroTimestamp(value)
      case _                  => Error(s"logical type $logicalTypeName currently not supported").asLeft
    }).fold(e => AvroFail(e.msg).asRight, _.asRight)

  private def typesFor(s: Schema) = s.getTypes.asScala.toVector

  private def typeListFor(s: Schema) = s.getTypes.asScala.toList

  private def fieldsFor(s: Schema) = s.getFields.asScala.toVector

}
