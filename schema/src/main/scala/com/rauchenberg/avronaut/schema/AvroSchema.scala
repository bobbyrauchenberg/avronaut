package com.rauchenberg.avronaut.schema

import java.time.OffsetDateTime

import cats.implicits._

import collection.JavaConverters._
import com.rauchenberg.avronaut.common._
import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations._
import com.rauchenberg.avronaut.schema.helpers.SchemaHelper._
import magnolia._
import org.apache.avro.{LogicalTypes, Schema, SchemaBuilder}
import shapeless.{:+:, CNil, Coproduct}

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

  implicit val stringSchema = new AvroSchema[String] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.stringType)
  }

  implicit val intSchema = new AvroSchema[Int] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.intType)
  }

  implicit val longSchema = new AvroSchema[Long] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.longType)
  }

  implicit val floatSchema = new AvroSchema[Float] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.floatType)
  }

  implicit val doubleSchema = new AvroSchema[Double] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.doubleType)
  }

  implicit val byteSchema = new AvroSchema[Array[Byte]] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.bytesType)
  }

  implicit def boolSchema = new AvroSchema[Boolean] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.booleanType)
  }

  implicit def nullSchema = new AvroSchema[Null] {
    override def schema: SchemaResult = safe(SchemaBuilder.builder.nullType)
  }

  implicit def mapSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Map[String, T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createMap(es)))
  }

  implicit def listSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[List[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }

  implicit def seqSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Seq[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }

  implicit def vectorSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Vector[T]] {
    override def schema: SchemaResult = elementSchema.schema.flatMap(es => safe(Schema.createArray(es)))
  }

  implicit def optionSchema[T](implicit elementSchema: AvroSchema[T]) = new AvroSchema[Option[T]] {
    override def schema: SchemaResult =
      elementSchema.schema.flatMap { es =>
        es.getType match {
          case Schema.Type.UNION =>
            safe(Schema.createUnion(SchemaBuilder.builder.nullType +: es.getTypes.asScala: _*))
          case _ => safe(Schema.createUnion(List(SchemaBuilder.builder.nullType, es).asJava))
        }
      }
  }

  implicit def eitherSchema[L, R](implicit leftSchema: AvroSchema[L], rightSchema: AvroSchema[R]) =
    new AvroSchema[Either[L, R]] {
      override def schema: SchemaResult =
        leftSchema.schema
          .map2(rightSchema.schema) { (l, r) =>
            {
              val lTypes = if (isUnion(l.getType)) l.getTypes.asScala else List(l)
              val rTypes = if (isUnion(r.getType)) r.getTypes.asScala else List(r)
              safe(Schema.createUnion((lTypes ++ rTypes): _*))
            }
          }
          .flatten
    }

  implicit def cnilSchema[H](implicit hSchema: AvroSchema[H]) = new AvroSchema[H :+: CNil] {
    override def schema: SchemaResult = hSchema.schema.flatMap(v => safe(Schema.createUnion(v)))

  }

  implicit def coproductSchema[H, T <: Coproduct](implicit hSchema: AvroSchema[H], tSchema: AvroSchema[T]) =
    new AvroSchema[H :+: T] {
      override def schema: SchemaResult =
        tSchema.schema
          .map2(hSchema.schema) { (l, r) =>
            if (r.getType == Schema.Type.UNION)
              safe(Schema.createUnion((l.getTypes.asScala.toList ++ r.getTypes.asScala).asJava))
            else safe(Schema.createUnion((l.getTypes.asScala.toList :+ r).asJava))
          }
          .flatten
    }

  implicit val dateTimeSchema = new AvroSchema[OffsetDateTime] {
    override def schema: SchemaResult = safe(LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder.longType))
  }

  private def isUnion(schemaType: Schema.Type) = schemaType == Schema.Type.UNION
}
