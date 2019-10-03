package com.rauchenberg.avronaut.encoder

import collection.JavaConverters._
import cats.instances.either._
import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.recursion.{FAlgebraM, FCoalgebraM, Fix, Morphisms}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

case class FoldingParser(genericRecord: GenericData.Record) {

  val toAvroF: FCoalgebraM[Result, AvroF, Avro] = {
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

  val toRawTypes: FAlgebraM[Result, AvroF, Any] = {
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

  def parse(avroType: Avro): Result[GenericData.Record] = {

    val runAna: Result[Fix[AvroF]] = Morphisms.anaM[Result, AvroF, Avro](toAvroF).apply(avroType)
    val toRaw                      = runAna.flatMap(v => Morphisms.cataM(toRawTypes).apply(v))

    //needs to be recursive
    def createRecord(gr: GenericData.Record, values: List[Any]) = {
      values.zipWithIndex.foreach {
        case (v, ind) =>
          v match {
            case AvroRecordF(schema, values) => gr.put(ind, values)
          }
      }
      gr
    }

    println(runAna)
    println(toRaw)
    genericRecord.asRight

  }

}
