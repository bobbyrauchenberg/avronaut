package com.rauchenberg.cupcatAvro.schema.annotations

import scala.annotation.StaticAnnotation

object SchemaAnnotations {

  sealed trait AnnotationKeys
  case object Name extends AnnotationKeys
  case object Namespace extends AnnotationKeys
  case object Doc extends AnnotationKeys
  case object Alias extends AnnotationKeys

  case class SchemaMetadata(values: Map[AnnotationKeys, String]) extends StaticAnnotation

  def getAnnotations(annotations: Seq[Any]): Option[SchemaMetadata] = annotations.toList.filter(_.isInstanceOf[SchemaMetadata]).headOption.map(_.asInstanceOf[SchemaMetadata])

  def getName(annotations: Option[SchemaMetadata], defaultName: String) = annotations.flatMap(_.values.get(Name)).getOrElse(defaultName)

  def getDoc(annotations: Option[SchemaMetadata]) = annotations.flatMap(_.values.get(Doc)).getOrElse("")

  def getNameAndNamespace(annotations: Option[SchemaMetadata], defaultName: String, defaultNamespace: String): (String, String) = {

    val name = getName(annotations, defaultName)
    val namespace = annotations.flatMap(_.values.get(Namespace))

    if(name.contains(".")) (name, defaultNamespace)
    else (name, namespace.getOrElse(defaultNamespace))
  }
}
