package core.impl

import core.api.SomeInterface
import core.api.camelCase

internal data class ImplType(
  override val name: String
) : SomeInterface {
  override val camelName: String = name.camelCase()
  val customName = name.customStuff()
}

internal fun String.customStuff() = "${hashCode()}"
