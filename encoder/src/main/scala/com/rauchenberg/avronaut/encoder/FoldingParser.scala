package com.rauchenberg.avronaut.encoder

import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import matryoshka.implicits._
import matryoshka.patterns._
import matryoshka.{AlgebraM, CoalgebraM, _}
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._

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
    case AvroArray(value)                 => AvroArrayF(null, value).asRight
    case AvroSchemaArray(schema, value)   => AvroArrayF(schema, value).asRight
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
    case AvroArray(value)                 => AvroArrayF(null, value)
    case AvroSchemaArray(schema, value)   => AvroArrayF(schema, value)
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
    case (index, AvroEnum(value))                => EnvT((index + 1, AvroEnumF(value.toString)))
    case (index, AvroUnion(value))               => EnvT((index + 1, AvroUnionF((index, value))))
    case (index, AvroArray(value))               => EnvT((index + 1, AvroArrayF(null, value.map(v => (index, v)))))
    case (index, AvroSchemaArray(schema, value)) => EnvT((index + 1, AvroArrayF(schema, value.map(v => (index, v)))))
    case (index, AvroMap(value))                 => EnvT((index + 1, AvroMapF(value.map { case (k, v) => k -> (index + 1, v) })))
    case (index, AvroBytes(value))               => EnvT((index + 1, AvroBytesF(value)))
    case (index, AvroLogical(value))             => EnvT((index + 1, AvroLogicalF((index, value))))
  }

  val toGR: Algebra[AvroF, List[(Int, Either[GenericData.Array[Any], GenericData.Record]) => Unit]] = {
    case AvroNullF =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) => r.fold(_.add(i, null), _.put(i, null))
      List(r)
    case AvroIntF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroDoubleF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroFloatF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroLongF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroBooleanF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroStringF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroRecordF(schema, value, isTop) =>
      val gr = if (isTop) genericRecord else new GenericData.Record(schema)
      val lu = schema.getFields.asScala.toList
        .zip(value)
        .zipWithIndex
        .map {
          case ((sf, func), i) =>
            if (func.isEmpty)
              gr.put(i, Nil.asJava)
            else {
              func.map { v =>
                v.apply(i, gr.asRight)
              }
            }
        }
      val f = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) => r.fold(_.add(i, gr), _.put(i, gr))
      List(f)
    case AvroEnumF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroUnionF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroArrayF(schema, value) =>
      val ga = new GenericData.Array[Any](value.size, schema)
      value.flatten.zipWithIndex.foreach {
        case (f, ind) => f(ind, ga.asLeft)
      }
      List((i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) => r.fold(_.add(i, ga), _.put(i, ga)))
    case AvroMapF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroBytesF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
      List(r)
    case AvroLogicalF(value) =>
      val r = (i: Int, r: Either[GenericData.Array[Any], GenericData.Record]) =>
        r.fold(_.add(i, value), _.put(i, value))
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
    case EnvT((ind, AvroArrayF(schema, value)))     => s"write array"
    case EnvT((ind, AvroMapF(value)))               => s"write map"
    case EnvT((ind, AvroBytesF(value)))             => s"write bytes"
    case EnvT((ind, AvroLogicalF(value)))           => s"write logical"
  }

  def parse(avroType: Avro): Result[GenericData.Record] = {
    println(avroType)
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
