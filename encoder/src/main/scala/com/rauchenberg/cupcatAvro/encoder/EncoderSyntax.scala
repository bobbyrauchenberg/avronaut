package com.rauchenberg.cupcatAvro.encoder

import cats.syntax.either._
import com.rauchenberg.cupcatAvro.common.{Error, Result}
import org.apache.avro.generic.GenericData

object EncoderSyntax {

  implicit class EncodedToGenericRecord(r: Result[Encoded]) {

    def toGenRecord: Either[Error, GenericData.Record] = r match {
      case Right(Elements(elements, schema)) => {
        val record = new GenericData.Record(schema)
        kestrel(record)(rec => elements.zipWithIndex.foreach { case (v, i) => parseAST(v, rec, i) }).asRight
      }
      case Left(error) => error.asLeft
      case _ =>
        Error("you can only convert Elements to a GenericData.Record").asLeft
    }
  }

  private def kestrel[T](x: T)(f: T => Unit) = { f(x); x }

  private def parseAST(e: Encoded, genRecord: GenericData.Record, cnt: Int): GenericData.Record = {
    val updateRecord = kestrel(genRecord) _
    e match {
      case Primitive(v) =>
        updateRecord(_.put(cnt, v))
      case Record(v) =>
        updateRecord(_.put(cnt, v))
      case Elements(e, schema) =>
        val record = new GenericData.Record(schema)
        e.zipWithIndex.foreach { case (enc, i) => parseAST(enc, record, i) }
        updateRecord(_.put(cnt, record))
    }
  }

}
