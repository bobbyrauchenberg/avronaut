package com.rauchenberg.avronaut.schema

import com.rauchenberg.avronaut.common.Result
import org.apache.avro.Schema

trait AvroSchema[A] {
  def data: Result[SchemaData]
}

object AvroSchema {
  def toSchema[A](implicit schemaBuilder: SchemaBuilder[A]): AvroSchema[A] = new AvroSchema[A] {
    override def data: Result[SchemaData] = schemaBuilder.schema.flatMap(Parser(_).parse)
  }
}

case class SchemaData(schemaMap: Map[String, Schema], schema: Schema)
