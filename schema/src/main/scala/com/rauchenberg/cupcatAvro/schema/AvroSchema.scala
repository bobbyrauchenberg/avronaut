package com.rauchenberg.cupcatAvro.schema

import cats.implicits._

import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations._
import org.apache.avro.Schema
import magnolia.{CaseClass, Magnolia, Param}
import SchemaHelper._

trait AvroSchema[T] {
  def schema: SchemaResult
}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](cc: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {

      val annotations = getAnnotations(cc.annotations)

      val (name, namespace) = getNameAndNamespace(annotations, cc.typeName.short, cc.typeName.owner)
      val doc = getDoc(annotations)

      cc.parameters.toList.traverse { param =>
        param.typeclass.schema.flatMap(mkField(_, param))
      }.flatMap(fields => schemaRecord(name, doc, namespace, false, fields))
    }
  }

  def mkField[T](schema: Schema, param: Param[AvroSchema.Typeclass, T]) = {

    val annotations = getAnnotations(param.annotations)

    val name = getName(annotations, param.label)
    val doc = getDoc(annotations)

    schema.getType match {
      case Schema.Type.UNION =>
        param.default.traverse { default =>
          moveDefaultToHead(schema, default, schemaFor(default))
            .map((_, default))
            .flatMap(v => schemaField(name, v._1, doc, extractDefault(v._2, schema)))
        }.flatMap(_.map(_.asRight).getOrElse(schemaField(name, schema, doc)))
      case _ =>
        param.default
          .traverse(default => schemaField(name, schema, doc, extractDefault(default, schema)))
          .flatMap(_.map(_.asRight).getOrElse(schemaField(name, schema, doc)))
    }


    }

}

