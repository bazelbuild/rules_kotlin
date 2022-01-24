package examples.android.lib

import com.google.auto.value.AutoValue

@AutoValue
abstract class TestKtValue {
  abstract fun name(): String
  internal fun nameInternal() = "${name()}_internal"

  @AutoValue.Builder
  abstract class Builder {
    abstract fun setName(name: String): Builder
    abstract fun build(): TestKtValue
  }
  companion object {
    fun create(builderFunction: Builder.() -> Unit) = AutoValue_TestKtValue.Builder().also {
        builderFunction.invoke(it)
      }.build()
  }

}
