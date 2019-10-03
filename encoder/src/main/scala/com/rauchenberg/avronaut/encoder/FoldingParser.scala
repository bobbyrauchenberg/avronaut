package com.rauchenberg.avronaut.encoder

import cats.instances.either._
import cats.syntax.either._
import com.rauchenberg.avronaut.common.AvroF._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.recursion.{FCoalgebraM, Morphisms}
import org.apache.avro.generic.GenericData

case class FoldingParser(genericRecord: GenericData.Record) {

  val toAvroF: FCoalgebraM[Result, AvroF, Avro] = {
    case AvroNull           => AvroNullF.asRight
    case AvroNumber(value)  => value.toInt.map(AvroIntF(_))
    case AvroBoolean(value) => AvroBooleanF(value).asRight
    case AvroString(value)  => AvroStringF(value).asRight
    case AvroRecord(value)  => AvroRecordF(value).asRight
    case AvroEnum(value)    => AvroEnumF(value.toString).asRight
    case AvroUnion(value)   => AvroUnionF(value).asRight
    case AvroArray(value)   => AvroArrayF(value).asRight
    case AvroMap(value)     => AvroMapF(value).asRight
    case AvroBytes(value)   => AvroBytesF(value).asRight
    case AvroLogical(value) => AvroLogicalF(value).asRight

  }

  def parse(avroType: Avro) = {

    val runAna = Morphisms.anaM[Result, AvroF, Avro](toAvroF).apply(avroType)

  }

}
