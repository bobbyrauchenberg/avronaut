package com.rauchenberg.cupcatAvro.schema.annotations

import scala.annotation.StaticAnnotation

object SchemaAnnotations {

  sealed trait AnnotationKeys
  case object Name extends AnnotationKeys
  case object Namespace extends AnnotationKeys
  case object Doc extends AnnotationKeys
  case object Alias extends AnnotationKeys

  case class SchemaMetadata(values: Map[AnnotationKeys, String]) extends StaticAnnotation

  def getAnnotations(annotations: Seq[Any]) = annotations.toList.filter(_.isInstanceOf[SchemaMetadata]).headOption.map(_.asInstanceOf[SchemaMetadata])

}
