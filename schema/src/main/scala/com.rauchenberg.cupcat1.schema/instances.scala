package com.rauchenberg.cupcat1.schema

import cats.syntax.either._
import org.apache.avro.{Schema, SchemaBuilder}

object instances {

  implicit val stringSchema = new AvroSchema[String] {
    override def schema: SchemaResult = SchemaBuilder.builder.stringType.asRight
  }

  implicit val intSchema = new AvroSchema[Int] {
    override def schema: SchemaResult = SchemaBuilder.builder.intType.asRight
  }

  implicit val longSchema = new AvroSchema[Long] {
    override def schema: SchemaResult = SchemaBuilder.builder.longType.asRight
  }

  implicit val floatSchema = new AvroSchema[Float] {
    override def schema: SchemaResult = SchemaBuilder.builder.floatType.asRight
  }

  implicit val doubleSchema = new AvroSchema[Double] {
    override def schema: SchemaResult = SchemaBuilder.builder.doubleType.asRight
  }

  implicit val byteSchema = new AvroSchema[Byte] {
    override def schema: SchemaResult = SchemaBuilder.builder.bytesType.asRight
  }

  implicit def boolSchema = new AvroSchema[Boolean] {
    override def schema: SchemaResult = SchemaBuilder.builder.booleanType.asRight
  }

  implicit def nullSchema = new AvroSchema[Null] {
    override def schema: SchemaResult = SchemaBuilder.builder.nullType.asRight
  }
}
