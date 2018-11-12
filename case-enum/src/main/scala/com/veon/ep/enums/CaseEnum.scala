package com.veon.ep.enums

import scala.reflect.runtime.universe._

@deprecatedInheritance("Manual implementations are deprecated, use `CaseEnumCompanion`.")
abstract class CaseEnum[T] {
  type Element = T

  val all: Set[T]

  def fromString(name: String): Option[T] = all.find(_.toString == name)
}

object CaseEnum {
  def apply[T: CaseEnum] = implicitly[CaseEnum[T]]
}

abstract class CaseEnumCompanion[E: WeakTypeTag] { self =>
  implicit object enum extends CaseEnum[E] {
    private def sealedDescendants: Set[Symbol] = {
      val symbol = weakTypeOf[E].typeSymbol
      val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]

      if (!symbol.isStatic)
        sealedError("non-static, probably inner 'path-dependent', symbol")
      else if (!internal.isSealed)
        sealedError("not sealed")
      else if (internal.sealedDescendants.size <= 1)
        sealedError("has no descendants")
      else
        internal.sealedDescendants.map(_.asInstanceOf[Symbol]) - symbol
    }

    private def sealedError(message: String) = {
      throw new IllegalStateException(
        s"Malformed enum ${weakTypeOf[E]}: $message. Enum should be statically accessible sealed type with `case object` descendants."
      )
    }

    lazy val all = sealedDescendants.map { symbol =>
      val module = symbol.owner.typeSignature.member(symbol.name.toTermName)
      if (!module.isModule)
        sealedError(s"descendant `$symbol` is not an object")
      else
        reflect.runtime.currentMirror.reflectModule(module.asModule).instance
    }.map(
      obj => obj.asInstanceOf[E]
    )
  }
}
