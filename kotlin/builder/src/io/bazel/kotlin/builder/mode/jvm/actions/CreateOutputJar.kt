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
package io.bazel.kotlin.builder.mode.jvm.actions

import io.bazel.kotlin.builder.BuildAction
import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.model.CompileDirectories
import io.bazel.kotlin.builder.utils.executeAndAwaitSuccess
import java.nio.file.Path

/**
 * Create a jar from all the input.
 */
class CreateOutputJar(toolchain: KotlinToolchain) : BuildAction("create output jar", toolchain) {
    private fun MutableList<String>.addAllFrom(dir: Path) = addAll(arrayOf("-C", dir.toString(), "."))

    private fun MutableList<String>.maybeAddAnnotationProcessingGeneratedClasses(ctx: Context) {
        ctx.flags.plugins?.let { pluginDescriptor ->
            CompileDirectories[ctx].annotionProcessingClasses.takeIf {
                pluginDescriptor.processors.isNotEmpty() && it.toFile().exists()
            }?.also { this.addAllFrom(it) }
        }
    }

    override fun invoke(ctx: Context): Int {
        try {
            mutableListOf(
                    toolchain.JAR_TOOL_PATH,
                    "cf", ctx.flags.outputClassJar
            ).also { args ->
                args.addAllFrom(CompileDirectories[ctx].classes)
                args.maybeAddAnnotationProcessingGeneratedClasses(ctx)
            }.also { executeAndAwaitSuccess(10, *it.toTypedArray()) }
        } catch (e: Exception) {
            throw RuntimeException("unable to create class jar", e)
        }
        return 0
    }
}
