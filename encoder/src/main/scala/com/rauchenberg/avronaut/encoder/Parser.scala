package com.rauchenberg.avronaut.encoder

import cats.implicits._
import com.rauchenberg.avronaut.common.AvroType._
import com.rauchenberg.avronaut.common.{AvroArray, AvroRecord, AvroType, AvroUnion, Error, Result}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type._
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

/**
  *
  * @param genericRecord this class has to interop with Java `GenericData.Record` which takes `Any` and mutates in place
  *
  *                      For efficiency, and to avoid `Any` return types, there is use of mutation
  *
  *                      There is no shared state
  *
  */
private[encoder] case class Parser(private[encoder] val genericRecord: GenericData.Record) {

  private var index = 0

  def parse(avroType: AvroRecord): Result[GenericData.Record] = parseRecord(genericRecord.getSchema, avroType)

  private def parseRecord(schema: Schema, avroRecord: AvroRecord): Result[GenericData.Record] = {
    schema.getFields.asScala.toList
      .zip(avroRecord.value)
      .foreach {
        case (field, recordField) => {
          parseType(field.schema, recordField)
        }
      }
    genericRecord.asRight
  }

  private def parseType(schema: Schema, avroType: AvroType): Result[Unit] = {
    val schemaType = schema.getType
    (schemaType, avroType) match {
      case (RECORD, v @ AvroRecord(_)) =>
        Parser(new GenericData.Record(schema)).parseRecord(schema, v).map { v =>
          genericRecord.put(index, v)
          index += 1
        }
      case (ARRAY, v @ AvroArray(_)) => parseArray(schema, v)
      case (UNION, v @ AvroUnion(_)) => parseUnion(schema, v)
      case (_, _)                    => addPrimitive(schema, avroType)
    }
  }

  private def parseArray(schema: Schema, avroArray: AvroArray): Result[Unit] =
    avroArray.value.traverse { value =>
      schema.getElementType.getType match {
        case ARRAY   => parseType(schema.getElementType, value)
        case UNION   => parseType(schema.getElementType, value)
        case RECORD  => addRecord(schema.getElementType, value)
        case STRING  => fromAvroString(value)
        case INT     => fromAvroInt(value)
        case BOOLEAN => fromAvroBoolean(value)
        case LONG    => fromAvroLong(value)
        case FLOAT   => fromAvroFloat(value)
        case DOUBLE  => fromAvroDouble(value)
        case BYTES   => fromAvroBytes(value)
        case NULL    => fromAvroNull(value)
        case _       => Error(s"couldn't map to AST for array, '$value', '$schema'").asLeft
      }
    }.map { list =>
      genericRecord.put(index, list.asJava)
      index += 1
    }

  private def parseUnion(schema: Schema, avroUnion: AvroUnion): Result[Unit] = {
    val initialIndex = index
    schema.getTypes.asScala.toList.foreach { schema =>
      schema.getType match {
        case UNION => parseType(schema, avroUnion.value)
        case ARRAY => parseType(schema, avroUnion.value)
        case RECORD =>
          addRecord(schema, avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case STRING =>
          fromAvroString(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case INT =>
          fromAvroInt(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case BOOLEAN =>
          fromAvroBoolean(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case LONG =>
          fromAvroLong(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case FLOAT =>
          fromAvroFloat(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case DOUBLE =>
          fromAvroDouble(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case BYTES =>
          fromAvroBytes(avroUnion.value).map { v =>
            genericRecord.put(index, v)
            index += 1
          }
        case NULL => fromAvroNull(avroUnion.value)
        case _    => Error(s"couldn't map to AST for array, '${avroUnion}', '$schema'").asLeft
      }
    }
    if (index > initialIndex) ().asRight[Error]
    else Error(s"couldn't parse Union, '${avroUnion.value}', '$schema'").asLeft
  }

  private def addRecord(schema: Schema, avroType: AvroType): Result[GenericData.Record] =
    avroType match {
      case a @ AvroRecord(_) =>
        Parser(new GenericData.Record(schema)).parseRecord(schema, a)
      case _ => Error(s"couldn't parseArray, expected an AvroRecord, '$avroType', '$schema'").asLeft
    }

  private def addPrimitive(schema: Schema, value: AvroType): Result[Unit] =
    (schema.getType match {
      case STRING  => fromAvroString(value)
      case INT     => fromAvroInt(value)
      case LONG    => fromAvroLong(value)
      case FLOAT   => fromAvroFloat(value)
      case DOUBLE  => fromAvroDouble(value)
      case BOOLEAN => fromAvroBoolean(value)
      case BYTES   => fromAvroBytes(value)
      case NULL    => fromAvroNull(value)
      case _       => Error(s"couldn't map to AST '$value', '$schema'").asLeft
    }).map { v =>
      genericRecord.put(index, v)
      index += 1
    }

}
