package com.rauchenberg.cupcatAvro

import com.rauchenberg.cupcatAvro.common._
import org.apache.avro.{Schema, SchemaBuilder}

import scala.collection.JavaConverters._

package object schema {

  type SchemaResult = Result[Schema]

  def schemaField[T](name: String, schema: Schema, doc: Option[String]) =
    safe(doc.fold(new Schema.Field(name, schema))(new Schema.Field(name, schema, _)))

  def schemaField[T](name: String, schema: Schema, doc: Option[String], default: T) =
    safe(new Schema.Field(name, schema, doc.getOrElse(""), default))

  def schemaRecord[T](name: String, doc: Option[String], namespace: String, fields: List[Schema.Field]) =
    safe(Schema.createRecord(name, doc.getOrElse(""), namespace, false, fields.asJava))

  def schemaEnum(name: String, namespace: String, doc: Option[String], symbols: List[String]) =
    safe(doc.fold(SchemaBuilder.builder.enumeration(name).namespace(namespace).symbols(symbols:_*)){ docValue =>
      SchemaBuilder.builder.enumeration(name).namespace(namespace).doc(docValue).symbols(symbols:_*)
    })

  def schemaUnion(types: List[Schema]) = safe(Schema.createUnion(types:_*))


}
