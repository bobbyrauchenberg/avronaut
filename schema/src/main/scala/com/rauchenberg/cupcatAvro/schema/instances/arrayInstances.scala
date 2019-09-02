package com.rauchenberg.cupcatAvro.schema.instances

import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult, safe}
import org.apache.avro.Schema

object arrayInstances extends arrayInstances

trait arrayInstances {

  implicit def listSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[List[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }

  implicit def seqSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Seq[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }

  implicit def vectorSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Vector[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }
}
