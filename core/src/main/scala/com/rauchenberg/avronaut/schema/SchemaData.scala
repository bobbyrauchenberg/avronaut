package com.rauchenberg.avronaut.schema

import org.apache.avro.Schema

case class SchemaData(schemaMap: Map[String, Schema], schema: Schema)
