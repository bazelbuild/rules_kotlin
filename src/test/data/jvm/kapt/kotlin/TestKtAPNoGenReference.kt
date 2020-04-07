/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.smoke.kapt.kotlin

import com.google.auto.value.AutoValue

@AutoValue
abstract class TestKtValueNoReferences {
  abstract fun name(): String

  @AutoValue.Builder
  abstract class Builder {
    abstract fun setName(name: String): Builder
    abstract fun build(): TestKtValueNoReferences
  }
}
