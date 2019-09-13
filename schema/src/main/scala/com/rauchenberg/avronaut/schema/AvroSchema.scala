package com.rauchenberg.avronaut.schema

import cats.implicits._
import collection.JavaConverters._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations._
import com.rauchenberg.avronaut.schema.helpers.SchemaHelper._
import magnolia._
import org.apache.avro.{Schema, SchemaBuilder}

trait AvroSchema[T] {
  def schema: SchemaResult
}

object AvroSchema {

  def apply[T](implicit avroSchema: AvroSchema[T]) = avroSchema

  type Typeclass[T] = AvroSchema[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    val annotations       = getAnnotations(ctx.annotations)
    val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)
    val toSchema          = schemaRecord(name, annotations.doc, namespace, _: List[Schema.Field])

    override def schema: SchemaResult =
      ctx.parameters.toList.traverse { param =>
        for {
          schema      <- param.typeclass.schema
          field       <- toField(param, namespace, schema)
          schemaField <- makeSchemaField(field)
        } yield schemaField
      }.flatMap(toSchema)
  }

  import reflect.runtime.universe._

  def dispatch[T : WeakTypeTag](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {

    val anno              = getAnnotations(ctx.annotations)
    val (name, namespace) = getNameAndNamespace(anno, ctx.typeName.short, ctx.typeName.full)

    val subtypes = ctx.subtypes.toList

    import scala.reflect.runtime.universe

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val tpe           = runtimeMirror.weakTypeOf[T]
    val isEnum        = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.knownDirectSubclasses.forall(_.isModuleClass)

    override def schema: SchemaResult =
      if (isEnum)
        schemaEnum(name, namespace, anno.doc, subtypes.map(_.typeName.short))
      else {
        subtypes.traverse(_.typeclass.schema).flatMap(schemaUnion)
      }

  }

  private def toField[T](param: Param[Typeclass, T], ccName: String, schema: Schema): Result[Field[T]] = {
    val annotations = getAnnotations(param.annotations)
    val name        = annotations.name(param.label)
    val namespace   = annotations.namespace(ccName)
    val default     = param.default.asInstanceOf[Option[T]]
    val doc         = annotations.doc

    val toField = Field(name, doc, _: Option[T], schema)

    schema.getType match {
      case Schema.Type.UNION =>
        default
          .traverse(moveDefaultToHead(schema, _).map(Field(name, doc, default, _)))
          .map(_.getOrElse(toField(None)))
      case Schema.Type.ENUM =>
        val enum = SchemaBuilder.builder
          .enumeration(schema.getName)
          .namespace(namespace)
          .symbols(schema.getEnumSymbols.asScala.toList: _*)
        doc.fold {
          Field(name, None, default, enum).asRight[Error]
        } { docValue =>
          Field(name, Option(docValue), default, enum).asRight[Error]
        }
      case _ => toField(default).asRight[Error]
    }
  }
}
