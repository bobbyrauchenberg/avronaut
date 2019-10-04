package com.rauchenberg.avronaut.decoder

import cats.implicits._
import collection.JavaConverters._
import scala.collection.convert.Wrappers.{MapWrapper, SeqWrapper}
import com.rauchenberg.avronaut.common.Avro._
import com.rauchenberg.avronaut.common._
import org.apache.avro.{LogicalType, Schema}
import org.apache.avro.Schema.Type._

import org.apache.avro.generic.GenericRecord

import scala.annotation.tailrec

object Parser {

  def parse(field: Schema.Field, genRec: GenericRecord) =
    parseTypes(field.schema, genRec.get(field.name))

  private def parseTypes[A](schema: Schema, value: A) = {
    val schemaType = schema.getType
    schemaType match {
      case RECORD => parseRecord(schema, value)
      case UNION  => parseUnion(schema, value)
      case ARRAY  => parseArray(schema, value)
      case ENUM   => AvroEnum(value).asRight
      case MAP    => parseMap(schema, value)
      case _      => toAST(schema.getLogicalType, schemaType, value)
    }
  }

  private def parseMap[A](schema: Schema, value: A): Result[Avro] =
    safe(
      value
        .asInstanceOf[MapWrapper[String, A]]
        .asScala
        .toList
        .traverse { case (k, v) => parseTypes(schema.getValueType, v).map(k -> _) }
        .map(AvroMap(_))).flatten

  private def parseArray[A](schema: Schema, value: A): Result[Avro] =
    valueToList(value).fold(
      _ => Error(s"parseArray can't cast '$value' to SeqWrapper for '$schema'").asLeft,
      _.traverse(parseTypes(schema.getElementType, _)).flatMap(toAvroArray(_))
    )

  private def parseUnion[A](schema: Schema, value: A): Result[Avro] = {

    val error = Error(s"couldn't parse union for '$value', '$schema'").asLeft[Avro]

    @tailrec
    def loop(schemas: List[Schema]): Result[Avro] = schemas match {

      case Nil => error
      case h :: t =>
        val schemaType = h.getType
        (schemaType match {
          case NULL  => toAvroNull(value)
          case ARRAY => parseArray(h, value)
          case ENUM  => toAvroEnum(value)
          case RECORD =>
            typesFrom(schema)
              .filter(_.getType == RECORD)
              .foldLeft(error) {
                case (r @ Right(_), _) => r
                case (acc, recordSchema) =>
                  parseRecord(recordSchema, value) match {
                    case r @ Right(AvroRecord(_, _, _)) => r
                    case _                              => acc
                  }
              }
          case _ =>
            toAST(schema.getLogicalType, schemaType, value)
        }) match {
          case Right(v) => v.asRight
          case _        => loop(t)
        }
    }

    def enumToEndOfUnion(schema: Schema) = {
      val schemaTypes = typesFrom(schema)
      schemaTypes.find(_.getType == ENUM).fold(schemaTypes) { enumSchema =>
        schemaTypes.filterNot(_.getType == ENUM) :+ enumSchema
      }
    }

    loop(enumToEndOfUnion(schema)).flatMap(toAvroUnion)
  }

  private def parseRecord[A](schema: Schema, value: A): Result[Avro] =
    value match {
      case gr: GenericRecord =>
        fieldsFrom(schema).traverse(field => parse(field, gr)).map(AvroRecord(null, _))
      case _ => Error(s"expected a GenericRecord for '$value', '$schema'").asLeft
    }

  private def toAST[A](logicalType: LogicalType, schemaType: Schema.Type, value: A) =
    Option(logicalType).fold(
      parsePrimitive(schemaType, value)
    ) { lt =>
      logicalTypeToPrimitive(lt.getName, value)
    }

  private def parsePrimitive[A](field: Schema.Type, value: A) =
    field match {
      case STRING  => toAvroString(value)
      case INT     => toAvroInt(value)
      case LONG    => toAvroLong(value)
      case FLOAT   => toAvroFloat(value)
      case DOUBLE  => toAvroDouble(value)
      case BOOLEAN => toAvroBoolean(value)
      case BYTES   => toAvroBytes(value)
      case NULL    => toAvroNull(value)
      case _       => Error(s"couldn't map to AST '$value', '$field'").asLeft
    }

  private def logicalTypeToPrimitive[A](logicalTypeName: String, value: A): Result[Avro] =
    logicalTypeName match {
      case "uuid"             => toAvroUUID(value)
      case "timestamp-millis" => toAvroTimestamp(value)
      case _                  => Error(s"logical type $logicalTypeName currently not supported").asLeft
    }

  private def fieldsFrom(s: Schema) = s.getFields.asScala.toList

  private def typesFrom(s: Schema) = s.getTypes.asScala.toList

  private def valueToList[A](value: A) = safe(value.asInstanceOf[SeqWrapper[A]].asScala.toList)

}
