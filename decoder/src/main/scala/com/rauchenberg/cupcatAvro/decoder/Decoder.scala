package com.rauchenberg.cupcatAvro.decoder

import collection.JavaConverters._
import com.rauchenberg.cupcatAvro.common._
import magnolia.{CaseClass, Magnolia, SealedTrait, Subtype}
import cats.implicits._
import com.rauchenberg.cupcatAvro.common.{Error, Result}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericContainer, GenericData, GenericEnumSymbol, GenericRecord}

import scala.reflect.runtime.universe

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
        val decodeResult = param.typeclass.decodeFrom(param.label, record)
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

      val recordValue = record.get(fieldName)

      val schema = record.getSchema.getField(fieldName).schema()
      schema.getType match {
        case Schema.Type.UNION =>
          recordValue match {
            case container: GenericData.Record =>
              val os = schema.getTypes.asScala.find(_.getFullName == container.getSchema.getFullName)
              val subschema = os.headOption
              ctx.subtypes.filter(_.typeName.full == container.getSchema.getFullName).headOption.flatMap { v: Subtype[Typeclass, T] =>
                subschema.map { ss =>
                  val gr = new GenericData.Record(ss)
                  container.getSchema.getFields.asScala.toList.traverse { f =>
                    safe(gr.put(f.name, container.get(f.name)))
                  }.flatMap(_ => v.typeclass.decodeFrom(fieldName, gr))
                    .leftMap(_ => Error("Was not able to create a sealed trait UNION, couldn't add fields to GenericData.Record"))
                }
              }.getOrElse(Error("Was not able to create a sealed trait UNION, couldn't find a subtype candidate").asLeft)
             case _ =>
               safe(
                 ctx.subtypes.filter(_.typeName.full == recordValue.asInstanceOf[Schema].getFullName).map(toCaseObject)
                 .headOption.map(_.asRight[Error]).getOrElse(Error(s"couldn't instantiate ENUM for field $fieldName").asLeft)
               ).flatten
          }
        case Schema.Type.ENUM =>
          ctx.subtypes.filter(_.typeName.short == recordValue).map(toCaseObject)
            .headOption.map(_.asRight[Error]).getOrElse(Error(s"couldn't instantiate ENUM for field $fieldName").asLeft)
        case other => Error(s"Unsupported sealed trait type : $other, only UNION and ENUM are supported").asLeft
      }

    }

    def toCaseObject(v: Subtype[Typeclass, T]) = {
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = runtimeMirror.staticModule(v.typeName.full)
      val companion = runtimeMirror.reflectModule(module.asModule)
      companion.instance.asInstanceOf[T]
    }

  }

}



