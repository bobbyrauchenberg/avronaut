package com.rauchenberg.avronaut.schema.instances

object schemaInstances extends schemaInstances

trait schemaInstances
    extends primitiveInstances
    with arrayInstances
    with mapInstances
    with unionInstances
    with logicalInstances
