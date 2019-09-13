package com.rauchenberg.avronaut.schema.instances

import java.time.OffsetDateTime

import com.rauchenberg.avronaut.common.safe
import com.rauchenberg.avronaut.schema.{AvroSchema, SchemaResult}
import org.apache.avro.{LogicalTypes, SchemaBuilder}

object logicalInstances extends logicalInstances

trait logicalInstances {

  implicit val dateTimeSchema = new AvroSchema[OffsetDateTime] {
    override def schema: SchemaResult = safe(LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder.longType))
  }

}
