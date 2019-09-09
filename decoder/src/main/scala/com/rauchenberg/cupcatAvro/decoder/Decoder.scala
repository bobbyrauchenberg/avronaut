package com.rauchenberg.cupcatAvro.decoder

import cats.implicits._
import com.rauchenberg.cupcatAvro.common.annotations.SchemaAnnotations._
import com.rauchenberg.cupcatAvro.common.{Error, Result, _}
import com.rauchenberg.cupcatAvro.decoder.helpers.ReflectionHelpers._
import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}

import scala.collection.JavaConverters._

trait Decoder[T] {

  def decodeFrom(fieldName: String, record: GenericRecord): Result[T]

}

object Decoder {

  def apply[T](implicit decoder: Decoder[T]) = decoder

  type Typeclass[T] = Decoder[T]

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[T] = {

      ctx.parameters.toList.traverse { param =>
        val annotations = getAnnotations(param.annotations)
        val decodeResult = param.typeclass.decodeFrom(getName(annotations, param.label), record)
        (decodeResult, param.default) match {
          case (Left(_), Some(default)) => default.asRight
          case (res, _) => res
        }
      }.map(ctx.rawConstruct(_))
    }
  }

  import scala.reflect.runtime.universe._

  def dispatch[T: TypeTag](ctx: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def decodeFrom(fieldName: String, record: GenericRecord): Result[T] = {

      val unionErrorMsg = "Was not able to create a sealed trait UNION"
      val enumErrorMsg = "couldn't instantiate ENUM for field"

      val recordValue = record.get(fieldName)
      val schema = record.getSchema.getField(fieldName).schema()

      def findSubtypeMatching(typeToMatch: String) = ctx.subtypes.filter(_.typeName.full == typeToMatch).headOption

      schema.getType match {
        case Schema.Type.UNION =>
          recordValue match {
            case container: GenericData.Record =>

              val recordSchema = container.getSchema
              val schemaFullName = recordSchema.getFullName
              val maybeSubschema = schema.getTypes.asScala.find(_.getFullName == schemaFullName).headOption

              def createSubRecord(subSchema: Schema): Either[Error, GenericData.Record] = {
                val gr = new GenericData.Record(subSchema)
                recordSchema.getFields.asScala.toList
                  .traverse(f => safe(gr.put(f.name, container.get(f.name)))).map(_ => gr)
              }

              (for {
                subSchema <- maybeSubschema
                subType <- findSubtypeMatching(schemaFullName)
              } yield {
                (for {
                  subRecord <- createSubRecord(subSchema)
                  decoded <- subType.typeclass.decodeFrom(fieldName, subRecord)
                } yield decoded).leftMap(_ => Error(s"$unionErrorMsg, couldn't create the GenericData.Record subrecord"))
              }).getOrElse(Error(s"$unionErrorMsg, couldn't find a subtype candidate").asLeft)

            case _ =>
              safe(
                findSubtypeMatching(recordValue.asInstanceOf[Schema].getFullName)
                  .map(st => toCaseObject[T](st.typeName.full))
                  .map(_.asRight[Error])
                  .getOrElse(Error(s"$enumErrorMsg $fieldName").asLeft)
              ).flatten
          }
        case Schema.Type.ENUM =>
          ctx.subtypes.filter(_.typeName.short == recordValue)
            .map(st => toCaseObject[T](st.typeName.full))
            .headOption.map(_.asRight[Error]).getOrElse(Error(s"$enumErrorMsg $fieldName").asLeft)
        case other => Error(s"Unsupported sealed trait type : $other, only UNION and ENUM are supported").asLeft
      }

    }

  }

}



