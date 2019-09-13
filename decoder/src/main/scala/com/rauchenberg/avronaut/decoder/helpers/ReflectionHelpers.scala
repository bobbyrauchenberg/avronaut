package com.rauchenberg.avronaut.decoder.helpers

import scala.reflect.runtime.universe
import reflect.runtime.universe._

object ReflectionHelpers {

  def toCaseObject[T](typeName: String): T = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module        = runtimeMirror.staticModule(typeName)
    val companion     = runtimeMirror.reflectModule(module.asModule)
    companion.instance.asInstanceOf[T]
  }

  def isOfType[T](name: String)(implicit tt: TypeTag[T]) =
    (tt.tpe match {
      case TypeRef(_, us, _) => us
    }).fullName == name

}
