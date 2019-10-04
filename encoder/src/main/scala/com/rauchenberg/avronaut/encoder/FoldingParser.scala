package com.rauchenberg.avronaut.encoder

import collection.JavaConverters._
import cats.instances.either._
import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common.{AvroNum, _}
import matryoshka._
import matryoshka.implicits._
import shims._
import matryoshka.data.Fix
import matryoshka.patterns._
import matryoshka.{AlgebraM, CoalgebraM}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

case class FoldingParser(genericRecord: GenericData.Record) {

  type WithIndex[A] = EnvT[Int, AvroF, A]

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

  //can use this to pass a schema about???
  val toAvroFEnv: Coalgebra[WithIndex, (Int, Avro)] = {
    case (index, AvroNull)           => EnvT((index + 1, AvroNullF))
    case (index, AvroNumber(value))  => EnvT((index + 1, AvroIntF(value.toInt.right.get)))
    case (index, AvroBoolean(value)) => EnvT((index + 1, AvroBooleanF(value)))
    case (index, AvroString(value))  => EnvT((index + 1, AvroStringF(value)))
    case (index, AvroRecord(schema, value, isTop)) =>
      EnvT((index, AvroRecordF(schema, value.map { v =>
        (index + 1, v)
      }, isTop)))
    case (index, AvroEnum(value))    => EnvT((index + 1, AvroEnumF(value.toString)))
    case (index, AvroUnion(value))   => EnvT((index + 1, AvroUnionF((index, value))))
    case (index, AvroArray(value))   => EnvT((index + 1, AvroArrayF(value.map(v => (index, v)))))
    case (index, AvroMap(value))     => EnvT((index + 1, AvroMapF(value.map { case (k, v) => k -> (index + 1, v) })))
    case (index, AvroBytes(value))   => EnvT((index + 1, AvroBytesF(value)))
    case (index, AvroLogical(value)) => EnvT((index + 1, AvroLogicalF((index, value))))
  }

  val toRawTypesM: AlgebraM[Result, AvroF, Any] = {
    case AvroNullF                         => Right(null)
    case AvroIntF(value)                   => value.asRight
    case AvroDoubleF(value)                => value.asRight
    case AvroFloatF(value)                 => value.asRight
    case AvroLongF(value)                  => value.asRight
    case AvroBooleanF(value)               => value.asRight
    case AvroStringF(value)                => value.asRight
    case AvroRecordF(schema, value, isTop) => AvroRecordF(schema, value, isTop).asRight
    case AvroEnumF(value)                  => value.asRight
    case AvroUnionF(value)                 => value.asRight
    case AvroArrayF(value)                 => value.asJava.asRight
    case AvroMapF(value)                   => value.toMap.asJava.asRight
    case AvroBytesF(value)                 => value.asRight
    case AvroLogicalF(value)               => value.asRight
  }

  val toRawTypes: Algebra[AvroF, Int] = {
    case AvroNullF                         => 1
    case AvroIntF(value)                   => 1
    case AvroDoubleF(value)                => 1
    case AvroFloatF(value)                 => 1
    case AvroLongF(value)                  => 1
    case AvroBooleanF(value)               => 1
    case AvroStringF(value)                => 1
    case AvroRecordF(schema, value, isTop) => value.sum
    case AvroEnumF(value)                  => 1
    case AvroUnionF(value)                 => 1
    case AvroArrayF(value)                 => value.sum
    case AvroMapF(value)                   => value.map(_._2).sum
    case AvroBytesF(value)                 => 1
    case AvroLogicalF(value)               => 1
  }

  val toGR: Algebra[AvroF, List[(Int, GenericData.Record) => Unit]] = {
    case AvroNullF =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, null)
      List(r)
    case AvroIntF(value) =>
      println("int: " + value)
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroDoubleF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroFloatF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroLongF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroBooleanF(value) =>
      println("boolean")
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroStringF(value) =>
      println("string : " + value)
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroRecordF(schema, value, isTop) =>
      println(schema)
      val gr = if (isTop) genericRecord else new GenericData.Record(schema)
      println(schema.getFields.asScala.toList)
      val lu = schema.getFields.asScala.toList
        .zip(value)
        .zipWithIndex
        .map {
          case ((sf, func), i) =>
            func.map { _.apply(i, gr) }
        }
        .flatten
      println("gr : " + gr)
      val f = (i: Int, r: GenericData.Record) => r.put(i, gr)
      List(f)
    case AvroEnumF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroUnionF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroArrayF(value) => value.flatten
    case AvroMapF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroBytesF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
    case AvroLogicalF(value) =>
      val r = (i: Int, r: GenericData.Record) => r.put(i, value)
      List(r)
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
