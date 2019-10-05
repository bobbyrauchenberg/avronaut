package com.rauchenberg.avronaut.encoder

import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import matryoshka.implicits._
import matryoshka.patterns._
import matryoshka.{AlgebraM, CoalgebraM, _}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

case class FoldingParser(genericRecord: GenericData.Record) {

  type WithIndex[A] = EnvT[Schema, AvroF, A]

  val toAvroFM: CoalgebraM[Result, AvroF, Avro] = {
    case AvroNull                         => AvroNullF.asRight
    case AvroNumber(value)                => value.toInt.map(AvroIntF(_))
    case AvroBoolean(value)               => AvroBooleanF(value).asRight
    case AvroString(value)                => AvroStringF(value).asRight
    case AvroRecord(schema, value, isTop) => AvroRecordF(schema, value, isTop).asRight
    case AvroEnum(value)                  => AvroEnumF(value.toString).asRight
    case AvroUnion(value)                 => AvroUnionF(value).asRight
    case AvroArray(value)                 => AvroArrayF(value).asRight
    case AvroMap(value)                   => AvroMapF(value).asRight
    case AvroBytes(value)                 => AvroBytesF(value).asRight
    case AvroLogical(value)               => AvroLogicalF(value).asRight
  }

  val toAvroF: Coalgebra[AvroF, Avro] = {
    case AvroNull                         => AvroNullF
    case AvroNumber(value)                => AvroIntF(value.toInt.right.get)
    case AvroBoolean(value)               => AvroBooleanF(value)
    case AvroString(value)                => AvroStringF(value)
    case AvroRecord(schema, value, isTop) => AvroRecordF(schema, value, isTop)
    case AvroEnum(value)                  => AvroEnumF(value.toString)
    case AvroUnion(value)                 => AvroUnionF(value)
    case AvroArray(value)                 => AvroArrayF(value)
    case AvroMap(value)                   => AvroMapF(value)
    case AvroBytes(value)                 => AvroBytesF(value)
    case AvroLogical(value)               => AvroLogicalF(value)
  }

//  //can use this to pass a schema about???
//  val toAvroFEnv: Coalgebra[WithIndex, (Schema, Avro)] = {
//    case (schema, AvroNull)           => EnvT((schema, AvroNullF))
//    case (schema, AvroNumber(value))  => EnvT((schema, AvroIntF(value.toInt.right.get)))
//    case (schema, AvroBoolean(value)) => EnvT((schema, AvroBooleanF(value)))
//    case (schema, AvroString(value))  => EnvT((schema, AvroStringF(value)))
//    case (schema, AvroRecord(s, value, isTop)) =>
//      EnvT((schema, AvroRecordF(schema, value.map { v =>
//        (schema, v)
//      }, isTop)))
//    case (schema, AvroEnum(value))        => EnvT((schema, AvroEnumF(value.toString)))
//    case (schema, AvroUnion(value))       => EnvT((schema, AvroUnionF((schema, value))))
//    case (schema, AvroArray(value)) => EnvT((s, AvroArrayF(value.map(v => (s, v)))))
//    case (schema, AvroMap(value))         => EnvT((schema, AvroMapF(value.map { case (k, v) => k -> (schema, v) })))
//    case (schema, AvroBytes(value))       => EnvT((schema, AvroBytesF(value)))
//    case (schema, AvroLogical(value))     => EnvT((schema, AvroLogicalF((schema, value))))
//  }

  val toGR: Algebra[AvroF, Any] = {
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
    case AvroRecordF(schema, values, isTop) =>
      val gr = if (isTop) genericRecord else new GenericData.Record(schema)
      val lu = schema.getFields.asScala.toList
        .zip(values)
        .zipWithIndex
        .map {
          case ((sf, value), i) =>
            gr.put(i, value)
        }
      gr
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

  val fromAvroEnv: Algebra[EnvT[Int, AvroF, ?], String] = {
    case EnvT((ind, AvroNullF))                     => null
    case EnvT((ind, AvroIntF(value)))               => s"write Int"
    case EnvT((ind, AvroDoubleF(value)))            => s"write Double"
    case EnvT((ind, AvroFloatF(value)))             => s"write Float"
    case EnvT((ind, AvroLongF(value)))              => s"write Long"
    case EnvT((ind, AvroBooleanF(value)))           => s"write bool"
    case EnvT((ind, AvroStringF(value)))            => s"write String"
    case EnvT((ind, AvroRecordF(schema, value, _))) => s"write Record"
    case EnvT((ind, AvroEnumF(value)))              => s"write Enum"
    case EnvT((ind, AvroUnionF(value)))             => s"write Union"
    case EnvT((ind, AvroArrayF(value)))             => s"write array"
    case EnvT((ind, AvroMapF(value)))               => s"write map"
    case EnvT((ind, AvroBytesF(value)))             => s"write bytes"
    case EnvT((ind, AvroLogicalF(value)))           => s"write logical"
  }

  def parse(avroType: Avro): Result[GenericData.Record] = {
    val funcs = avroType.hylo(toGR, toAvroF)

//
//
//
//        println(avroType.hylo(toRawTypes, toAvroF))
//
//
//
    genericRecord.asRight
//
  }

}
