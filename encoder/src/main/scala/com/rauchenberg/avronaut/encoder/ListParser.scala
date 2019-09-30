package com.rauchenberg.avronaut.encoder

import collection.JavaConverters._
import cats.implicits._
import com.rauchenberg.avronaut.common.Avro.{
  fromAvroBoolean,
  fromAvroBytes,
  fromAvroDouble,
  fromAvroFloat,
  fromAvroInt,
  fromAvroLong,
  fromAvroNull,
  fromAvroString
}
import com.rauchenberg.avronaut.common.{Avro, AvroArray, AvroRecord, AvroUnion, Error, Result}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type.{ARRAY, BOOLEAN, BYTES, DOUBLE, FLOAT, INT, LONG, NULL, RECORD, STRING, UNION}
import org.apache.avro.generic.GenericData

case class ListParser(val schema: Schema, array: AvroArray) {

  def parse = {
    val res = array.value.traverse { value =>
      parseType(schema, value)
    }.map(_.asJava)
    res
  }

  private def parseType(schema: Schema, avroType: Avro) = {
    val schemaType = schema.getType
    (schemaType, avroType) match {
      case (RECORD, v @ AvroRecord(_)) =>
        Parser(new GenericData.Record(schema)).parse(v)
      case (ARRAY, v @ AvroArray(_)) => parseArray(schema, v)
      case (UNION, v @ AvroUnion(_)) => parseUnion(schema, v)
      case (_, _)                    => addPrimitive(schema.getElementType, avroType)
    }
  }

  private def addPrimitive(schema: Schema, value: Avro): Result[Any] =
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

  private def parseUnion(schema: Schema, avroUnion: AvroUnion): Result[Any] =
    schema.getTypes.asScala.toList.traverse { schema =>
      schema.getType match {
        case UNION => parseType(schema, avroUnion.value)
        case ARRAY => parseType(schema, avroUnion.value)
        case RECORD =>
          parseType(schema, avroUnion.value)
        case STRING =>
          fromAvroString(avroUnion.value)
        case INT =>
          fromAvroInt(avroUnion.value)
        case BOOLEAN =>
          fromAvroBoolean(avroUnion.value)
        case LONG =>
          fromAvroLong(avroUnion.value)
        case FLOAT =>
          fromAvroFloat(avroUnion.value)
        case DOUBLE =>
          fromAvroDouble(avroUnion.value)
        case BYTES =>
          fromAvroBytes(avroUnion.value)
        case NULL => fromAvroNull(avroUnion.value)
        case _    => Error(s"couldn't map to AST for array, '${avroUnion}', '$schema'").asLeft
      }
    }

  private def parseArray(schema: Schema, avroArray: AvroArray): Result[java.util.List[Any]] =
    schema.getElementType.getType match {
      case ARRAY =>
        ListParser(schema.getElementType, avroArray).parse
      case _ =>
        avroArray.value.traverse { value =>
          schema.getElementType.getType match {
            case UNION   => parseType(schema.getElementType, value)
            case RECORD  => parseType(schema.getElementType, value)
            case STRING  => fromAvroString(value)
            case INT     => fromAvroInt(value)
            case BOOLEAN => fromAvroBoolean(value)
            case LONG    => fromAvroLong(value)
            case FLOAT   => fromAvroFloat(value)
            case DOUBLE  => fromAvroDouble(value)
            case BYTES   => fromAvroBytes(value)
            case NULL    => fromAvroNull(value)
            case ARRAY   => ListParser(schema.getElementType, avroArray).parse
            case _       => Error(s"couldn't map to AST for array, '$value' '$schema'").asLeft
          }
        }.map(_.asJava)
    }

}
