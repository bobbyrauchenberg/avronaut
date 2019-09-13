package com.rauchenberg.avronaut.schema.instances

import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaResult}
import org.apache.avro.Schema
import com.rauchenberg.avronaut.common._

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
