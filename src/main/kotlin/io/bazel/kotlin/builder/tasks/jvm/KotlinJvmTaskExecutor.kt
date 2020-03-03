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

import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KaptCompilerPluginArgsEncoder
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.model.JvmCompilationTask
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Due to an inconsistency in the handling of -Xfriends-path, jvm uses a comma (property list
 * separator), js uses the system path separator.
 */
const val X_FRIENDS_PATH_SEPARATOR = ","

@Singleton
class KotlinJvmTaskExecutor @Inject internal constructor(
  private val compiler: KotlinToolchain.KotlincInvoker,
  private val pluginArgsEncoderKapt: KaptCompilerPluginArgsEncoder,
  private val javaCompiler: JavaCompiler,
  private val jDepsGenerator: JDepsGenerator,
  private val abiPlugins: KtAbiPluginArgs
) {

  private fun combine(one: Throwable?, two: Throwable?): Throwable? {
    return when {
      one != null && two != null -> {
        one.addSuppressed(two)
        return one
      }
      one != null -> one
      else -> two
    }
  }

  fun execute(context: CompilationTaskContext, task: JvmCompilationTask) {
    val preprocessedTask = task
      .preProcessingSteps(context)
      .runPlugins(context, pluginArgsEncoderKapt, compiler)

    context.execute("compile classes") {
      preprocessedTask.apply {
        sequenceOf(
            runCatching {
              context.execute("kotlinc") {
                compileKotlin(context,
                    compiler,
                    args = baseArgs()
                        .given(outputs.jar).notEmpty {
                          append(codeGenArgs())
                        }.given(outputs.abijar).notEmpty {
                          plugin(abiPlugins.jvmAbiGen) {
                            flag("outputDir", directories.classes)
                          }
                          given(outputs.jar).empty {
                            plugin(abiPlugins.skipCodeGen)
                          }
                        },
                    printOnFail = false
                )
              }
            },
            runCatching {
              context.execute("javac") {
                if (outputs.jar.isNotEmpty()) {
                  javaCompiler.compile(context, this)
                } else {
                  emptyList()
                }
              }
            }
        ).map {
          (it.getOrNull() ?: emptyList()) to it.exceptionOrNull()
        }.map {
          when (it.second) {
            // TODO(issue/296): remove when the CompilationStatusException is unified.
            is CompilationStatusException ->
              (it.second as CompilationStatusException).lines + it.first to it.second
            else -> it
          }
        }.fold(Pair<List<String>, Throwable?>(emptyList(), null)) { acc, result ->
          acc.first + result.first to combine(acc.second, result.second)
        }.apply {
          first.apply(context::printCompilerOutput)
          second?.let {
            throw it
          }
        }

        if (outputs.jar.isNotEmpty()) {
          context.execute("create jar", ::createOutputJar)
          context.execute("produce src jar", ::produceSourceJar)
        }
        if (outputs.abijar.isNotEmpty()) {
          context.execute("create abi jar", ::createAbiJar)
        }
        if (outputs.jdeps.isNotEmpty()) {
          context.execute("generate jdeps") { jDepsGenerator.generateJDeps(this) }
        }
      }
    }
  }
}
