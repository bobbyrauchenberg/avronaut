package com.rauchenberg.avronaut.encoder

import shims._

import collection.JavaConverters._
import cats.syntax.either._
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import japgolly.microlibs.recursion.Recursion._
import japgolly.microlibs.recursion.{FAlgebraM, FCoalgebraM}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

case class Parser(genericRecord: GenericData.Record) {

  def parse(avroType: Avro): Result[GenericData.Record] = {
    hyloM[Result, AvroF, Avro, Any](toAvroF, populateGenericRecord)(avroType)
    genericRecord.asRight
  }

  val toAvroF: FCoalgebraM[Result, AvroF, Avro] = {
    case AvroPrimitiveArray(value) => AvroPrimitiveArrayF(value).asRight
    case AvroNull                  => AvroNullF.asRight
    case AvroInt(value)            => AvroIntF(value).asRight
    case AvroDouble(value)         => AvroDoubleF(value).asRight
    case AvroLong(value)           => AvroLongF(value).asRight
    case AvroFloat(value)          => AvroFloatF(value).asRight
    case AvroBoolean(value)        => AvroBooleanF(value).asRight
    case AvroString(value)         => AvroStringF(value).asRight
    case AvroEnum(value)           => AvroEnumF(value.toString).asRight
    case AvroUnion(value)          => AvroUnionF(value).asRight
    case AvroArray(value)          => AvroArrayF(value).asRight
    case AvroMap(value)            => AvroMapF(value).asRight
    case AvroBytes(value)          => AvroBytesF(value).asRight
    case AvroLogical(value)        => AvroLogicalF(value).asRight
    case AvroRecord(schema, value) => AvroRecordF(schema, value).asRight
    case AvroRoot(schema, value)   => AvroRootF(schema, value).asRight
    case AvroDecode                => Error("should never encode an AvroDecode").asLeft
    case AvroError(msg)            => Error(s"got an error during encoding, '$msg'").asLeft
  }

  val populateGenericRecord: FAlgebraM[Result, AvroF, Any] = {
    case AvroPrimitiveArrayF(value) => value.asRight
    case AvroNullF                  => Right(null)
    case AvroIntF(value)            => value.asRight
    case AvroDoubleF(value)         => value.asRight
    case AvroFloatF(value)          => value.asRight
    case AvroLongF(value)           => value.asRight
    case AvroBooleanF(value)        => value.asRight
    case AvroStringF(value)         => value.asRight
    case AvroRecordF(schema, values) =>
      val genericRecord = new GenericData.Record(schema)
      addToRecord(schema, genericRecord, values).map(_ => genericRecord)
    case AvroRootF(schema, values) =>
      addToRecord(schema, genericRecord, values).map(_ => genericRecord)
    case AvroEnumF(value)    => value.asRight
    case AvroUnionF(value)   => value.asRight
    case AvroArrayF(value)   => safe(value.asJava)
    case AvroMapF(value)     => value.toMap.asJava.asRight
    case AvroBytesF(value)   => value.asRight
    case AvroLogicalF(value) => value.asRight
    case AvroErrorF(_)       => throw new RuntimeException("this is just for testing")
  }

  private def addToRecord[A](schema: Schema, genericRecord: GenericData.Record, values: List[A]): Result[List[Unit]] =
    schema.getFields.asScala.toList
      .zip(values)
      .zipWithIndex
      .traverse {
        case ((_, value), i) => safe(genericRecord.put(i, value))
      }

}
