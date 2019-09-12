package com.rauchenberg.cupcatAvro.schema.instances

import java.time.OffsetDateTime

import com.rauchenberg.cupcatAvro.common.safe
import com.rauchenberg.cupcatAvro.schema.{AvroSchema, SchemaResult}
import org.apache.avro.{LogicalTypes, SchemaBuilder}

object logicalInstances extends logicalInstances

trait logicalInstances {

  implicit val dateTimeSchema = new AvroSchema[OffsetDateTime] {
    override def schema: SchemaResult = safe(LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder.longType))
  }

}
