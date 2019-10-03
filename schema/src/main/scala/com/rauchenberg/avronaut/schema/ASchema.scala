//package com.rauchenberg.avronaut.schema
//
//abstract class Schema                                     extends Product with Serializable
//final case class SchemaString(value: String)              extends Schema
//final case class SchemaInt(value: Int)                    extends Schema
//final case class SchemaBool(value: Boolean)               extends Schema
//final case class SchemaArray(value: List[Schema])         extends Schema
//final case class SchemaMap(value: List[(String, Schema)]) extends Schema
//final case class SchemaEnum(value: String)                extends Schema
//final case class SchemaUnion(value: List[Schema])         extends Schema
//final case class SchemaRecord(value: List[Schema])        extends Schema
//
//sealed trait SchemaF[A]
//final case class SchemaStringF(value: String)            extends SchemaF[Nothing]
//final case class SchemaIntF(value: Int)                  extends SchemaF[Nothing]
//final case class SchemaBoolF(value: Boolean)             extends SchemaF[Nothing]
//final case class SchemaArrayF[A](value: A)               extends SchemaF[A]
//final case class SchemaMapF[A](value: List[(String, A)]) extends SchemaF[A]
//final case class SchemaEnumF(value: String)              extends SchemaF[Nothing]
//final case class SchemaUnionF[A](value: List[A])         extends SchemaF[A]
//final case class SchemaRecordF[A](value: List[A])        extends SchemaF[A]
