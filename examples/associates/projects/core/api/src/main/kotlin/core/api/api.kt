package core.api

interface SomeInterface {
  val name: String
  val camelName: String
}

data class MyType(
  override val name: String
) : SomeInterface {
  override val camelName: String = name.camelCase()
}

internal fun String.camelCase() = this.split("_").joinToString("") {
  "${it[0].toUpperCase()}${it.substring(1)}"
}

sealed class Result {
  class Success(): Result()
  class Failure(): Result()
}
