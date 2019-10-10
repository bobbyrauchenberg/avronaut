package com.rauchenberg.avronaut.common

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

object ReflectionHelpers {

  def toCaseObject[A](typeName: String): A = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module        = runtimeMirror.staticModule(typeName)
    val companion     = runtimeMirror.reflectModule(module.asModule)
    companion.instance.asInstanceOf[A]
  }

  def isEnum[A : WeakTypeTag] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val tpe           = runtimeMirror.weakTypeOf[A]
    tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.knownDirectSubclasses.forall(_.isModuleClass)
  }
}
