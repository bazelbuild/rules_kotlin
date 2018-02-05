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
package io.bazel.ruleskotlin.workers.compilers.jvm.actions


import io.bazel.ruleskotlin.workers.BuildAction
import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.KotlinToolchain
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.Flags
import io.bazel.ruleskotlin.workers.model.Metas
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Should be the first step, does mandatory pre-processing.
 */
class Initialize(toolchain: KotlinToolchain) : BuildAction("initialize KotlinBuilder", toolchain) {
    override fun invoke(ctx: Context): Int {
        ctx.apply(
                ::initializeAndBindBindDirectories,
                ::bindLabelComponents,
                ::bindSources
        )
        return 0
    }

    private fun bindSources(ctx: Context) {
        val javaSources = mutableListOf<String>()
        val allSources = mutableListOf<String>()
        for (src in requireNotNull(Flags.SOURCES[ctx]).split(":")) {
            when {
                src.endsWith(".java") -> {
                    javaSources.add(src)
                    allSources.add(src)
                }
                src.endsWith(".kt") -> allSources.add(src)
                else -> throw RuntimeException("unrecognised file type: $src")
            }
        }
        Metas.JAVA_SOURCES[ctx] = javaSources.toList()
        Metas.ALL_SOURCES[ctx] = allSources.toList()
    }

    private fun initializeAndBindBindDirectories(ctx: Context) {
        Files.createDirectories(Paths.get(Flags.COMPILER_OUTPUT_BASE[ctx])).let {
            CompileDirectories[ctx] = CompileDirectories(it)
        }
    }

    /**
     * parses the label, sets up the meta elements and returns the target part.
     */
    private fun bindLabelComponents(ctx: Context) {
        val label = requireNotNull(Flags.LABEL[ctx])
        val parts = label.split(":")
        require(parts.size == 2) { "the label $label is invalid" }
        Metas.PKG[ctx] = parts[0]
        Metas.TARGET[ctx] = parts[1]
    }
}
