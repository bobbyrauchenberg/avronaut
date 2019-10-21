package com.rauchenberg.avronaut.schema

import com.rauchenberg.avronaut.common.Results

trait AvroSchema[A] {
  def data: Results[SchemaData]
}

object AvroSchema {
  def toSchema[A](implicit schemaBuilder: SchemaBuilder[A]): AvroSchema[A] = new AvroSchema[A] {
    override val data: Results[SchemaData] = schemaBuilder.schema.flatMap(Parser(_).parse)
  }
}
