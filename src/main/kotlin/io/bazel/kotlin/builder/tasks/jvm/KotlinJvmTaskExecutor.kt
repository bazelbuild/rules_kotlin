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

import io.bazel.kotlin.builder.toolchain.BtapiRuntimeSpec
import io.bazel.kotlin.builder.toolchain.BtapiToolchainsCache
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.model.JvmCompilationTask
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalBuildToolsApi::class)
class KotlinJvmTaskExecutor
  @JvmOverloads
  constructor(
    private val defaultRuntimeSpec: BtapiRuntimeSpec? = null,
    private val defaultPlugins: InternalCompilerPlugins? = null,
    private val btapiToolchainsCache: BtapiToolchainsCache = BtapiToolchainsCache(),
  ) {
    private val compilers = ConcurrentHashMap<BtapiRuntimeSpec, BtapiCompiler>()

    fun execute(
      context: CompilationTaskContext,
      task: JvmCompilationTask,
    ) {
      val runtimeSpec =
        defaultRuntimeSpec ?: error(
          "Btapi runtime spec must be provided either in KotlinJvmTaskExecutor constructor " +
            "or per execute(...) call.",
        )
      val plugins =
        defaultPlugins ?: error(
          "Internal compiler plugins must be provided either in " +
            "KotlinJvmTaskExecutor constructor or per execute(...) call.",
        )
      execute(context, task, runtimeSpec, plugins)
    }

    fun execute(
      context: CompilationTaskContext,
      task: JvmCompilationTask,
      runtimeSpec: BtapiRuntimeSpec,
      plugins: InternalCompilerPlugins,
    ) {
      val btapiCompiler =
        compilers.computeIfAbsent(runtimeSpec) {
          BtapiCompiler(btapiToolchainsCache.get(it))
        }

      val preprocessedTask =
        task
          .preProcessingSteps(context)
          .runPlugins(context, plugins, btapiCompiler)

      context.execute("compile classes") {
        preprocessedTask.apply {
          context.execute("kotlinc") {
            if (compileKotlin && inputs.kotlinSourcesList.isNotEmpty()) {
              val result = btapiCompiler.compile(this, plugins, context.out)
              when (result) {
                CompilationResult.COMPILATION_SUCCESS -> { /* success */ }
                CompilationResult.COMPILATION_ERROR ->
                  throw CompilationStatusException("Compilation failed", 1)
                CompilationResult.COMPILATION_OOM_ERROR ->
                  throw CompilationStatusException("Compilation failed with OOM", 3)
                CompilationResult.COMPILER_INTERNAL_ERROR ->
                  throw CompilationStatusException("Compiler internal error", 2)
              }
            }
          }

          // Ensure jdeps exists even if the compiler did not emit it.
          context.execute("ensure jdeps") { ensureJdepsExists() }

          if (outputs.jar.isNotEmpty()) {
            if (instrumentCoverage) {
              context.execute("create instrumented jar", ::createCoverageInstrumentedJar)
            } else {
              context.execute("create jar", ::createOutputJar)
            }
          }
          if (outputs.abijar.isNotEmpty()) {
            context.execute("create abi jar", ::createAbiJar)
          }
          if (outputs.generatedJavaSrcJar.isNotEmpty()) {
            context.execute("creating KAPT generated Java source jar", ::createGeneratedJavaSrcJar)
          }
          if (outputs.generatedJavaStubJar.isNotEmpty()) {
            context.execute("creating KAPT generated Kotlin stubs jar", ::createGeneratedStubJar)
          }
          if (outputs.generatedClassJar.isNotEmpty()) {
            context.execute("creating KAPT generated stub class jar", ::createGeneratedClassJar)
          }
          if (outputs.generatedKspSrcJar.isNotEmpty()) {
            context.execute("creating KSP generated src jar", ::createGeneratedKspKotlinSrcJar)
          }
          if (outputs.generatedKspClassesJar.isNotEmpty()) {
            context.execute("creating KSP generated classes jar", ::createdGeneratedKspClassesJar)
          }
        }
      }
    }
  }
