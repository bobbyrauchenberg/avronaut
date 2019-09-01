package com.rauchenberg.cupcatAvro.schema

import com.rauchenberg.cupcatAvro.schema.AvroSchema.SchemaResult
import org.apache.avro.SchemaBuilder

object instances {

  implicit val stringSchema = new AvroSchema[String] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.stringType)
  }

  implicit val intSchema = new AvroSchema[Int] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.intType)
  }

  implicit val longSchema = new AvroSchema[Long] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.longType)
  }

  implicit val floatSchema = new AvroSchema[Float] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.floatType)
  }

  implicit val doubleSchema = new AvroSchema[Double] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.doubleType)
  }

  implicit val byteSchema = new AvroSchema[Byte] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.bytesType)
  }

  implicit def boolSchema = new AvroSchema[Boolean] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.booleanType)
  }

  implicit def nullSchema = new AvroSchema[Null] {
    override def schema: SchemaResult = safeSchema(SchemaBuilder.builder.nullType)
  }

}
