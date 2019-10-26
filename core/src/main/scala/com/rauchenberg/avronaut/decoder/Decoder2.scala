//package com.rauchenberg.avronaut.decoder
//
//import cats.data.Reader
//import com.rauchenberg.avronaut.common.annotations.SchemaAnnotations.{getAnnotations, getNameAndNamespace}
//import com.rauchenberg.avronaut.common.{Avro, Result}
//import com.rauchenberg.avronaut.schema.SchemaData
//import magnolia.{CaseClass, Magnolia}
//import org.apache.avro.generic.GenericRecord
//
//trait Decoder2[A] {
//
//  def apply(genericRecord: GenericRecord, schemaData: SchemaData): A
//
//}
//
//object Decoder2 {
//
//  def apply[A](implicit decoder: Decoder2[A]) = decoder
//
//  type Typeclass[A] = Decoder2[A]
//
//  implicit def gen[A]: Decoder2[A] = macro Magnolia.gen[A]
//
//  def combine[A](ctx: CaseClass[Typeclass, A]): Decoder2[A] = {
//
//    val params = ctx.parameters.toList
//
//    val annotations       = getAnnotations(ctx.annotations)
//    val (name, namespace) = getNameAndNamespace(annotations, ctx.typeName.short, ctx.typeName.owner)
//
//    new Decoder2[A] {
//      override def apply(genericRecord: GenericRecord, schemaData: SchemaData): A = {
//        params.map { param =>
//          val paramAnnotations = getAnnotations(param.annotations)
//          val paramName        = paramAnnotations.name(param.label)
//
//          schemaData.schemaMap.get(s"$namespace.$name").toList.map { schema =>
//
//          }
//        }
//      }
//    }
//  }
//}
