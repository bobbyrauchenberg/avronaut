package com.rauchenberg.avronaut.encoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common.{AvroArray, AvroRecord, AvroType, Error, Result}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._
import org.apache.avro.generic.{GenericData, GenericRecord}

import scala.collection.JavaConverters._

object Parser {

  def parse(schema: Schema, avroType: AvroRecord) = {
    val genericRecord = new GenericData.Record(schema)
    parseRecord(schema, avroType, genericRecord)
  }

  def parseRecord(schema: Schema, avroRecord: AvroRecord, genericRecord: GenericData.Record): Result[GenericRecord] = {
    schema.getFields.asScala.toList
      .zip(avroRecord.value)
      .zipWithIndex
      .foreach {
        case ((field, recordField), ind) => {
          parseType(field.schema, recordField).map(genericRecord.put(ind, _))
        }
      }
    println("genericRecord : " + genericRecord)
    genericRecord.asRight
  }

  private def parseType(schema: Schema, avroType: AvroType) = {
    val schemaType = schema.getType
    (schemaType, avroType) match {
      case (RECORD, v @ AvroRecord(_)) => parseRecord(schema, v, new GenericData.Record(schema))
      case (ARRAY, v @ AvroArray(_))   => parseArray(schema, v).map(_.asJava)
      case (_, _)                      => parsePrimitive(schema, avroType)
    }
  }

  private def parseArray(schema: Schema, avroArray: AvroArray): Result[List[_]] =
    avroArray.value.traverse { value =>
      parsePrimitive(schema.getElementType, value)
    }

  private def parsePrimitive(schema: Schema, value: AvroType): Result[_] =
    schema.getType match {
      case STRING  => fromAvroString(value)
      case INT     => fromAvroInt(value)
      case LONG    => fromAvroLong(value)
      case FLOAT   => fromAvroFloat(value)
      case DOUBLE  => fromAvroDouble(value)
      case BOOLEAN => fromAvroBoolean(value)
      case BYTES   => fromAvroBytes(value)
      case NULL    => fromAvroNull(value)
      case _       => Error(s"couldn't map to AST '$value', '$schema'").asLeft
    }
}
