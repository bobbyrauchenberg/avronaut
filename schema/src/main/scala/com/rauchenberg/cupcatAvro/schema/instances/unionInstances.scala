package com.rauchenberg.cupcatAvro.schema.instances

import scala.collection.JavaConverters._
import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult, safe}
import org.apache.avro.{Schema, SchemaBuilder}

trait unionInstances {

  implicit def optionSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Option[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap { es =>
      safe(Schema.createUnion(List(SchemaBuilder.builder.nullType, es).asJava))
    }
  }

}
