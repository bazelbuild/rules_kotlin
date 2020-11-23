package examples.android.lib

import com.google.auto.value.AutoValue

@AutoValue
abstract class TestKtValue {
  abstract fun name(): String
  fun builder(): Builder = AutoValue_TestKtValue.Builder()

  @AutoValue.Builder
  abstract class Builder {
    abstract fun setName(name: String): Builder
    abstract fun build(): TestKtValue
  }
}
