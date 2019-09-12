package com.rauchenberg.cupcatAvro.encoder

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common.{Error, Result}
import org.apache.avro.generic.GenericData

object EncoderSyntax {

  implicit class EncodedToGenericRecord(r: Result[Encoded]) {

    def toGenRecord: Either[Error, GenericData.Record] = r match {
      case Right(Elements(elements, schema)) => {
        val record = new GenericData.Record(schema)
        elements.zipWithIndex.foreach { case (v, i) => parseAST(v, record, i) }
        record.asRight
      }
      case Left(error) => error.asLeft
      case _ =>
        Error("you can only convert Elements to a GenericData.Record").asLeft
    }
  }

  private def parseAST(e: Encoded, acc: GenericData.Record, cnt: Int): GenericData.Record =
    e match {
      case Primitive(p) =>
        acc.put(cnt, p)
        acc
      case Record(g) =>
        acc.put(cnt, g)
        acc
      case Elements(e, schema) =>
        val record = new GenericData.Record(schema)
        e.zipWithIndex.foreach { case (enc, i) => parseAST(enc, record, i) }
        acc.put(cnt, record)
        acc
    }
}
