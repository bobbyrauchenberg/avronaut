package com.rauchenberg.avronaut.schema

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

trait TypeInfo[A] { type Name <: String; def name: String }

object TypeInfo {
  type Aux[A, Name0 <: String] = TypeInfo[A] { type Name = Name0 }

  def apply[A](implicit ti: TypeInfo[A]): Aux[A, ti.Name] = ti

  implicit def materializeTypeInfo[A, Name <: String]: Aux[A, Name] =
    macro matTypeInfoImpl[A, Name]

  def matTypeInfoImpl[A : c.WeakTypeTag, Name <: String](c: whitebox.Context): c.Tree = {
    import c.universe._

    val A    = c.weakTypeOf[A]
    val name = A.typeSymbol.asType.fullName

    q"new TypeInfo[$A] { type Name = ${Constant(name)}; def name = $name }"
  }
}
