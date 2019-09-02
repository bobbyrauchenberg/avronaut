package com.rauchenberg.cupcatAvro.schema

import cats.implicits._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations.SchemaMetadata
import org.apache.avro.Schema
import magnolia.{CaseClass, Magnolia, Param}

import scala.collection.JavaConverters._

trait AvroSchema[T] {

  def schema: SchemaResult

}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](cc: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {

      val annotations = SchemaAnnotations.getAnnotations(cc.annotations)
      val namespace = annotations.flatMap(_.values.get(SchemaAnnotations.Namespace))

      cc.parameters.toList.traverse { param =>
        param.typeclass.schema.flatMap { schema =>
          mkField(schema, cc, param, namespace)
        }
      }.map { fields =>
        Schema.createRecord(cc.typeName.short, "", namespace.getOrElse(""), false, fields.asJava)
      }
    }
  }

  def mkField[T](schema: Schema, cc: CaseClass[Typeclass, T], param: Param[AvroSchema.Typeclass, T], namespace: Option[String]) = {
    val annotation: Option[SchemaMetadata] = SchemaAnnotations.getAnnotations(param.annotations)

    val name = annotation.flatMap(_.values.get(SchemaAnnotations.Name)).getOrElse(param.label)
    val doc = annotation.flatMap(_.values.get(SchemaAnnotations.Doc)).getOrElse("")

    param.default.traverse { default =>
        safeSchema(new Schema.Field(name, schema, doc, default))
      }.flatMap { withDefault =>
        withDefault.map(_.asRight).getOrElse(safeSchema(new Schema.Field(name, schema, doc)))
      }
  }



}

