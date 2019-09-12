package com.rauchenberg.cupcatAvro.encoder

import com.rauchenberg.cupcatAvro.common.{Error, Result}
import org.apache.avro.generic.GenericData
import cats.syntax.either._

object EncoderSyntax {

  implicit class EncodedToGenericRecord(r: Result[Encoded]) {

    def toGenRecord: Either[Error, GenericData.Record] = r match {
      case Right(Elements(elements, schema)) => {
        val record = new GenericData.Record(schema)
        val parsed = elements.flatMap(v => parseAST(v, Nil))
        parsed.zipWithIndex.foreach { case (r, i) => record.put(i, r) }
        r.map(_ => record)
      }
      case Left(error) => error.asLeft
      case _ =>
        Error("you can only convert Elements to a GenericData.Record").asLeft
    }
  }

  private def parseAST(e: Encoded, acc: List[Any]): List[Any] = e match {
    case Primitive(p) => acc :+ p
    case Record(g)    => acc :+ g
    case Elements(e, schema) =>
      val record = new GenericData.Record(schema)
      e.flatMap(v => parseAST(v, acc)).zipWithIndex.foreach {
        case (r, i) => record.put(i, r)
      }
      acc :+ record
  }
}
