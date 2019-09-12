package com.rauchenberg.cupcatAvro.schema.instances

object schemaInstances extends schemaInstances

trait schemaInstances
    extends primitiveInstances
    with arrayInstances
    with mapInstances
    with unionInstances
    with logicalInstances
