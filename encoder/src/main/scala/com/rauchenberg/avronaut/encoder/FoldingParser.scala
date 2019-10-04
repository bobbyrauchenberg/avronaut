package com.rauchenberg.avronaut.encoder

import collection.JavaConverters._
import cats.instances.either._
import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
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

  val toAvroFEnv: Coalgebra[WithIndex, (Int, AvroFix)] = {
    case (index, Fix(AvroNullF))           => EnvT((index, AvroNullF))
    case (index, Fix(AvroIntF(value)))     => EnvT((index, AvroIntF(value)))
    case (index, Fix(AvroBooleanF(value))) => EnvT((index, AvroBooleanF(value)))
    case (index, Fix(AvroStringF(value)))  => EnvT((index, AvroStringF(value)))
    case (index, Fix(AvroRecordF(schema, value))) =>
      EnvT((index, AvroRecordF(schema, value.map { v =>
        (index, v)
      })))
    case (index, Fix(AvroEnumF(value)))    => EnvT((index, AvroEnumF(value)))
    case (index, Fix(AvroUnionF(value)))   => EnvT((index, AvroUnionF((index, value))))
    case (index, Fix(AvroArrayF(value)))   => EnvT((index, AvroArrayF(value.map(v => (index, v)))))
    case (index, Fix(AvroMapF(value)))     => EnvT((index, AvroMapF(value.map { case (k, v) => k -> (index, v) })))
    case (index, Fix(AvroBytesF(value)))   => EnvT((index, AvroBytesF(value)))
    case (index, Fix(AvroLogicalF(value))) => EnvT((index, AvroLogicalF((index, value))))
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

  def parse(avroType: Avro): Result[GenericData.Record] = {



    val simpleAna = avroType.ana[AvroFix](toAvroF)
    simpleAna.hylo()

    println(simpleAna)
    val runAna = avroType.anaM[AvroFix](toAvroFM)

    val toRaw = runAna.flatMap(v => v.cataM(toRawTypesM))

//    println(runAna)
//    println(toRaw)
    genericRecord.asRight

  }

}
