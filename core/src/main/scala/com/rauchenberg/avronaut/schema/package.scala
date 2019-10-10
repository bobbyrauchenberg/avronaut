package com.rauchenberg.avronaut

import cats.data.NonEmptyList
import com.rauchenberg.avronaut.common._
import org.apache.avro.{Schema, SchemaBuilder}

import scala.collection.JavaConverters._

package object schema {

  type SchemaResult = Result[Schema]

  def schemaField(name: String, schema: Schema, doc: Option[String]): Result[Schema.Field] =
    safe(doc.fold(new Schema.Field(name, schema))(new Schema.Field(name, schema, _)))

  def schemaField[A](name: String, schema: Schema, doc: Option[String], default: A) =
    safe(new Schema.Field(name, schema, doc.getOrElse(""), default))

  def schemaRecord[A](name: String, doc: Option[String], namespace: String, fields: List[Schema.Field]) =
    safe(Schema.createRecord(name, doc.getOrElse(""), namespace, false, fields.asJava))

  def schemaEnum(name: String, namespace: String, doc: Option[String], symbols: NonEmptyList[String]) =
    safe(doc.fold(SchemaBuilder.builder.enumeration(name).namespace(namespace).symbols(symbols.toList: _*)) {
      docValue =>
        SchemaBuilder.builder.enumeration(name).namespace(namespace).doc(docValue).symbols(symbols.toList: _*)
    })

  def schemaUnion(types: List[Schema]) = safe(Schema.createUnion(types: _*))
}
