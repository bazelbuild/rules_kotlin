/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.builder.tasks.jvm

data class InternalCompilerPlugin(
  val jarPath: String,
  val id: String,
)

class InternalCompilerPlugins constructor(
  val jvmAbiGen: InternalCompilerPlugin,
  val skipCodeGen: InternalCompilerPlugin,
  val kapt: InternalCompilerPlugin,
  val jdeps: InternalCompilerPlugin,
) {
  companion object {
    const val JVM_ABI_GEN_ID = "org.jetbrains.kotlin.jvm.abi"
    const val SKIP_CODE_GEN_ID = "io.bazel.kotlin.plugin.SkipCodeGen"
    const val KAPT_ID = "org.jetbrains.kotlin.kapt3"
    const val JDEPS_ID = "io.bazel.kotlin.plugin.jdeps.JDepsGen"

    @JvmStatic
    fun fromPaths(
      jvmAbiGenJar: String,
      skipCodeGenJar: String,
      kaptJar: String,
      jdepsJar: String,
    ): InternalCompilerPlugins =
      InternalCompilerPlugins(
        jvmAbiGen = InternalCompilerPlugin(jvmAbiGenJar, JVM_ABI_GEN_ID),
        skipCodeGen = InternalCompilerPlugin(skipCodeGenJar, SKIP_CODE_GEN_ID),
        kapt = InternalCompilerPlugin(kaptJar, KAPT_ID),
        jdeps = InternalCompilerPlugin(jdepsJar, JDEPS_ID),
      )
  }
}
