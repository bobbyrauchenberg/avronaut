package com.rauchenberg.cupcatAvro.common.annotations

import scala.annotation.StaticAnnotation

object SchemaAnnotations {

  sealed trait AnnotationKeys
  case object Name extends AnnotationKeys
  case object Namespace extends AnnotationKeys
  case object Doc extends AnnotationKeys
  case object Alias extends AnnotationKeys

  case class SchemaMetadata(values: Map[AnnotationKeys, String]) extends StaticAnnotation

  def getAnnotations(annotations: Seq[Any]): Option[SchemaMetadata] =
    annotations.toList.find(_.isInstanceOf[SchemaMetadata]).map(_.asInstanceOf[SchemaMetadata])

  def getNameAndNamespace(annotations: Option[SchemaMetadata], defaultName: String, defaultNamespace: String): (String, String) = {

    val name = getName(annotations, defaultName)
    val namespace = getNamespace(annotations, defaultNamespace)

    if(name.contains(".")) (name, defaultNamespace) else (name, namespace)
  }

  def getName(annotations: Option[SchemaMetadata], defaultName: String) =
    annotations.flatMap(_.values.get(Name)).getOrElse(defaultName)

  private def getNamespace(annotations: Option[SchemaMetadata], defaultNamespace: String) =
    annotations.flatMap(_.values.get(Namespace)).getOrElse(defaultNamespace)

  private def getDoc(annotations: Option[SchemaMetadata]): Option[String] = annotations.flatMap(_.values.get(Doc))

  implicit class AnnotationValue(val annotations: Option[SchemaMetadata]) extends AnyVal {
    def doc = getDoc(annotations)
    def name(default: String) = getName(annotations, default)
    def namespace(default: String) = getNamespace(annotations, default)
  }

}
