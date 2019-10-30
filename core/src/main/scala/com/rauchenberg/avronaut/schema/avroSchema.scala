package com.rauchenberg.avronaut.schema

import com.rauchenberg.avronaut.common.Results
import org.apache.avro.Schema

trait AvroSchema[A] {
  def data: Results[SchemaData]
}

object AvroSchema {
  def toSchema[A](implicit schemaBuilder: SchemaBuilder[A]): AvroSchema[A] = new AvroSchema[A] {
    override val data: Results[SchemaData] = schemaBuilder.schema.flatMap(Parser(_).parse)
  }
}

case class SchemaData(schemaMap: Map[String, Schema], schema: Schema)
