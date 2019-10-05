package com.rauchenberg.avronaut.encoder

import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import matryoshka.implicits._
import matryoshka.patterns._
import matryoshka.{CoalgebraM, _}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

sealed trait TypeHolder[+A] {
  def getValue: A
}
case class Holder[A](value: A) extends TypeHolder[A] {
  def getValue = value
}

case class FoldingParser(genericRecord: GenericData.Record) {

  type WithIndex[A] = EnvT[Schema, AvroF, A]

  val toAvroFM: CoalgebraM[Result, AvroF, Avro] = {
    case AvroNull                  => AvroNullF.asRight
    case AvroNumber(value)         => value.toInt.map(AvroIntF(_))
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
  }

  val toAvroF: Coalgebra[AvroF, Avro] = {
    case AvroNull                  => AvroNullF
    case AvroNumber(value)         => AvroIntF(value.toInt.right.get)
    case AvroBoolean(value)        => AvroBooleanF(value)
    case AvroString(value)         => AvroStringF(value)
    case AvroEnum(value)           => AvroEnumF(value.toString)
    case AvroUnion(value)          => AvroUnionF(value)
    case AvroArray(value)          => AvroArrayF(value)
    case AvroMap(value)            => AvroMapF(value)
    case AvroBytes(value)          => AvroBytesF(value)
    case AvroLogical(value)        => AvroLogicalF(value)
    case AvroRecord(schema, value) => AvroRecordF(schema, value)
    case AvroRoot(schema, value)   => AvroRootF(schema, value)
  }

  val populateGenericRecord: Algebra[AvroF, Any] = {
    case AvroNullF => null
    case AvroIntF(value) =>
      value
    case AvroDoubleF(value) =>
      value
    case AvroFloatF(value) =>
      value
    case AvroLongF(value) =>
      value
    case AvroBooleanF(value) =>
      value
    case AvroStringF(value) =>
      value
    case AvroRecordF(schema, values) =>
      val genericRecord = new GenericData.Record(schema)
      addToRecord(schema, genericRecord, values)
      genericRecord
    case AvroRootF(schema, values) =>
      addToRecord(schema, genericRecord, values)
    case AvroEnumF(value) =>
      value
    case AvroUnionF(value) =>
      value
    case AvroArrayF(value) =>
      value.asJava
    case AvroMapF(value) =>
      value
    case AvroBytesF(value) =>
      value
    case AvroLogicalF(value) =>
      value
  }

  def parse(avroType: Avro): Result[GenericData.Record] = {
    avroType.hylo(populateGenericRecord, toAvroF)
    genericRecord.asRight
  }

  private def addToRecord[A](schema: Schema, genericRecord: GenericData.Record, values: List[A]) =
    schema.getFields.asScala.toList
      .zip(values)
      .zipWithIndex
      .map {
        case ((sf, value), i) =>
          genericRecord.put(i, value)
      }

}
