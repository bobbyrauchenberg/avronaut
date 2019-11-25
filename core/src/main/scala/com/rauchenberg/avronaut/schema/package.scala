package com.rauchenberg.avronaut

import cats.data.NonEmptyList
import com.rauchenberg.avronaut.common._
import org.apache.avro.{Schema, SchemaBuilder => AvroSchemaBuilder}

import scala.collection.JavaConverters._

package object schema {

  type SchemaResult = Results[Schema]

  def schemaField(name: String, schema: Schema, doc: Option[String]): Results[Schema.Field] =
    safeL(doc.fold(new Schema.Field(name, schema))(new Schema.Field(name, schema, _)))

  def schemaField[A](name: String, schema: Schema, doc: Option[String], default: A) =
    safeL(new Schema.Field(name, schema, doc.orNull, default))

  def schemaRecord[A](name: String, doc: Option[String], namespace: String, fields: List[Schema.Field]) =
    safeL(Schema.createRecord(name, doc.orNull, namespace, false, fields.asJava))

  def schemaEnum(name: String, namespace: String, doc: Option[String], symbols: NonEmptyList[String]) =
    safeL(doc.fold(AvroSchemaBuilder.builder.enumeration(name).namespace(namespace).symbols(symbols.toList: _*)) {
      docValue =>
        AvroSchemaBuilder.builder.enumeration(name).namespace(namespace).doc(docValue).symbols(symbols.toList: _*)
    })

  def schemaUnion(types: List[Schema]) = safeL(Schema.createUnion(types: _*))
}
