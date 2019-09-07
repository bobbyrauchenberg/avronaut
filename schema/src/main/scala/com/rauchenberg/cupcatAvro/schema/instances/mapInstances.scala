package com.rauchenberg.cupcatAvro.schema.instances

import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult}
import org.apache.avro.Schema
import com.rauchenberg.cupcatAvro.common._

object mapInstances extends mapInstances

trait mapInstances {

  implicit def mapSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Map[String, T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createMap(es)))
  }

}
