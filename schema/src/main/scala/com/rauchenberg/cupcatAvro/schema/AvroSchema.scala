package com.rauchenberg.cupcatAvro.schema

import cats.implicits._
import com.rauchenberg.cupcatAvro.common._

import scala.collection.JavaConverters._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations._
import com.rauchenberg.cupcatAvro.schema.helpers.SchemaHelper._
import org.apache.avro.Schema
import magnolia._

trait AvroSchema[T] {
  def schema: SchemaResult
}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    val annotations = getAnnotations(ctx.annotations)
    val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)
    val toSchema = schemaRecord(name, annotations.doc, namespace, false, _: List[Schema.Field])

    override def schema: SchemaResult = {
      ctx.parameters.toList.traverse { param =>
        for {
          schema <- param.typeclass.schema
          field <- toField(param, schema)
          schemaField <- makeSchemaField(field)
        } yield schemaField
      }.flatMap(toSchema)
    }
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    val anno = getAnnotations(ctx.annotations)
    val subtypes = ctx.subtypes.map(_.cast.asInstanceOf[Subtype[Typeclass, T]]).toList

    override def schema: SchemaResult = {
      def toEnumOrUnion(schemas: List[Schema]) =
        if (schemas.flatMap(_.getFields.asScala.toList).isEmpty)
          schemaEnum(ctx.typeName.owner, anno.namespace(ctx.typeName.owner), anno.doc, subtypes.map(_.typeName.short))
        else schemaUnion(schemas)

      for {
        schemas <- subtypes.traverse(_.typeclass.schema)
        enumOrUnion <- toEnumOrUnion(schemas)
      } yield enumOrUnion
    }
  }

  private def toField[T](param: Param[Typeclass, T], schema: Schema): Result[Field[T]] = {
    val annotations = getAnnotations(param.annotations)
    val name = annotations.name(param.label)
    val default = param.default.asInstanceOf[Option[T]]
    val doc = annotations.doc

    val toField = Field(name, doc, _: Option[T], schema)

    schema.getType match {
      case Schema.Type.UNION =>
        default.traverse(moveDefaultToHead(schema, _).map(Field(name, doc, default, _))).map(_.getOrElse(toField(None)))
      case _ => toField(default).asRight[Error]
    }
  }


}

