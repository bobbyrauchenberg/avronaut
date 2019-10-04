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
    case AvroNull                  => AvroNullF.asRight
    case AvroNumber(value)         => value.toInt.map(AvroIntF(_))
    case AvroBoolean(value)        => AvroBooleanF(value).asRight
    case AvroString(value)         => AvroStringF(value).asRight
    case AvroRecord(schema, value) => AvroRecordF(schema, value).asRight
    case AvroEnum(value)           => AvroEnumF(value.toString).asRight
    case AvroUnion(value)          => AvroUnionF(value).asRight
    case AvroArray(value)          => AvroArrayF(value).asRight
    case AvroMap(value)            => AvroMapF(value).asRight
    case AvroBytes(value)          => AvroBytesF(value).asRight
    case AvroLogical(value)        => AvroLogicalF(value).asRight
  }

  val toAvroF: Coalgebra[AvroF, Avro] = {
    case AvroNull                  => AvroNullF
    case AvroNumber(value)         => AvroIntF(123)
    case AvroBoolean(value)        => AvroBooleanF(value)
    case AvroString(value)         => AvroStringF(value)
    case AvroRecord(schema, value) => AvroRecordF(schema, value)
    case AvroEnum(value)           => AvroEnumF(value.toString)
    case AvroUnion(value)          => AvroUnionF(value)
    case AvroArray(value)          => AvroArrayF(value)
    case AvroMap(value)            => AvroMapF(value)
    case AvroBytes(value)          => AvroBytesF(value)
    case AvroLogical(value)        => AvroLogicalF(value)
  }

  val toAvroFEnv: Coalgebra[WithIndex, (Int, Avro)] = {
    case (index, AvroNull)           => EnvT((index + 1, AvroNullF))
    case (index, AvroNumber(_))      => EnvT((index + 1, AvroIntF(123)))
    case (index, AvroBoolean(value)) => EnvT((index + 1, AvroBooleanF(value)))
    case (index, AvroString(value))  => EnvT((index + 1, AvroStringF(value)))
    case (index, AvroRecord(schema, value)) =>
      EnvT((index, AvroRecordF(schema, value.map { v =>
        (index + 1, v)
      })))
    case (index, AvroEnum(value))    => EnvT((index + 1, AvroEnumF(value.toString)))
    case (index, AvroUnion(value))   => EnvT((index + 1, AvroUnionF((index, value))))
    case (index, AvroArray(value))   => EnvT((index + 1, AvroArrayF(value.map(v => (index, v)))))
    case (index, AvroMap(value))     => EnvT((index + 1, AvroMapF(value.map { case (k, v) => k -> (index + 1, v) })))
    case (index, AvroBytes(value))   => EnvT((index + 1, AvroBytesF(value)))
    case (index, AvroLogical(value)) => EnvT((index + 1, AvroLogicalF((index, value))))
  }

  val toRawTypesM: AlgebraM[Result, AvroF, Any] = {
    case AvroNullF                  => Right(null)
    case AvroIntF(value)            => value.asRight
    case AvroDoubleF(value)         => value.asRight
    case AvroFloatF(value)          => value.asRight
    case AvroLongF(value)           => value.asRight
    case AvroBooleanF(value)        => value.asRight
    case AvroStringF(value)         => value.asRight
    case AvroRecordF(schema, value) => AvroRecordF(schema, value).asRight
    case AvroEnumF(value)           => value.asRight
    case AvroUnionF(value)          => value.asRight
    case AvroArrayF(value)          => value.asJava.asRight
    case AvroMapF(value)            => value.toMap.asJava.asRight
    case AvroBytesF(value)          => value.asRight
    case AvroLogicalF(value)        => value.asRight
  }

  val toRawTypes: Algebra[AvroF, Any] = {
    case AvroNullF                  => null
    case AvroIntF(value)            => value
    case AvroDoubleF(value)         => value
    case AvroFloatF(value)          => value
    case AvroLongF(value)           => value
    case AvroBooleanF(value)        => value
    case AvroStringF(value)         => value
    case AvroRecordF(schema, value) => AvroRecordF(schema, value)
    case AvroEnumF(value)           => value
    case AvroUnionF(value)          => value
    case AvroArrayF(value)          => value.asJava
    case AvroMapF(value)            => value.toMap.asJava
    case AvroBytesF(value)          => value
    case AvroLogicalF(value)        => value
  }

  val fromAvroEnv: Algebra[EnvT[Int, AvroF, ?], String] = {
    case EnvT((ind, AvroNullF))                  => null
    case EnvT((ind, AvroIntF(value)))            => s"$ind"
    case EnvT((ind, AvroDoubleF(value)))         => s"$ind"
    case EnvT((ind, AvroFloatF(value)))          => s"$ind"
    case EnvT((ind, AvroLongF(value)))           => s"$ind"
    case EnvT((ind, AvroBooleanF(value)))        => s"$ind"
    case EnvT((ind, AvroStringF(value)))         => s"$ind"
    case EnvT((ind, AvroRecordF(schema, value))) => s"$ind"
    case EnvT((ind, AvroEnumF(value)))           => s"$ind"
    case EnvT((ind, AvroUnionF(value)))          => s"$ind"
    case EnvT((ind, AvroArrayF(value)))          => s"$ind"
    case EnvT((ind, AvroMapF(value)))            => s"$ind"
    case EnvT((ind, AvroBytesF(value)))          => s"$ind"
    case EnvT((ind, AvroLogicalF(value)))        => s"$ind"
  }

  def parse(avroType: Avro): Result[GenericData.Record] = {

    //val simpleAna = avroType.ana[AvroFix](toAvroF)
    val simpleAna = (0, avroType).hylo(fromAvroEnv, toAvroFEnv)

    println(simpleAna)

//    println(runAna)
//    println(toRaw)
    genericRecord.asRight

  }

}
