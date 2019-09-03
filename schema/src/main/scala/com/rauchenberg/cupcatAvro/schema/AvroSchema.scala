package com.rauchenberg.cupcatAvro.schema

import cats.implicits._
import collection.JavaConverters._
import com.rauchenberg.cupcatAvro.schema.annotations.SchemaAnnotations._
import com.rauchenberg.cupcatAvro.schema.helpers.AvroHelper._
import com.rauchenberg.cupcatAvro.schema.helpers.SchemaHelper._
import magnolia.{CaseClass, Magnolia, Param, SealedTrait, Subtype}
import org.apache.avro.{Schema, SchemaBuilder}

case class Field[T](name: String, doc: String, default: Option[T], schema: Schema)

trait AvroSchema[T] {
  def schema: SchemaResult
}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def schema: SchemaResult = {

      val annotations = getAnnotations(ctx.annotations)
      val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)
      val doc = getDoc(annotations)
      ctx.parameters.toList.traverse { param =>
        for {
          schema <- param.typeclass.schema
          field <- toField(param, schema)
          schemaField <- makeSchemaField(field)
        } yield schemaField
      }.flatMap(fields => schemaRecord(name, doc, namespace, false, fields))
    }
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T])(): Typeclass[T] = new Typeclass[T] {

    val annotations = getAnnotations(ctx.annotations)
    val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)
    val doc = getDoc(annotations)

    override def schema: SchemaResult = {
      val subtypes = ctx.subtypes.map { st =>
        st.cast.asInstanceOf[Subtype[Typeclass, T]]
      }.toList
      subtypes.traverse { v =>
        v.typeclass.schema
      }.flatMap { schemas =>
        if(isEnum(schemas)) {
          val subtypeSymbols = subtypes.map(_.typeName.short)
          subtypes.headOption.map { typeName =>
            safe(SchemaBuilder.builder.enumeration(typeName.typeName.owner).namespace(namespace).doc(doc).symbols(subtypeSymbols:_*))
          }.getOrElse(SchemaError("could fine no subtypes to build enum").asLeft)
        } else {
          safe(Schema.createUnion(schemas:_*))
        }
      }
    }
  }

  private def toField[T](param: Param[Typeclass, T], schema: Schema): Either[SchemaError, Field[T]] = {
    val annotations = getAnnotations(param.annotations)
    val name = getName(annotations, param.label)
    val doc = getDoc(annotations)
    val default = param.default.asInstanceOf[Option[T]]

    schema.getType match {
      case Schema.Type.UNION =>
        default.traverse { defaultValue =>
          moveDefaultToHead(schema, defaultValue, avroTypeFor(default)).map(Field(name, doc, default, _))
        }.map(_.getOrElse(Field(name, doc, None, schema)))
      case _ => Field(name, doc, default, schema).asRight[SchemaError]
    }
  }


}

