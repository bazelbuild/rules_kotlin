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
import io.bazel.kotlin.builder.model.CompileDependencies
import io.bazel.kotlin.builder.model.CompilePluginConfig
import io.bazel.kotlin.builder.model.Metas
import io.bazel.kotlin.builder.utils.PluginArgs

/**
 * Should be the first step, does mandatory pre-processing.
 */
class Initialize(toolchain: KotlinToolchain) : BuildAction("initialize KotlinBuilder", toolchain) {
    override fun invoke(ctx: Context): Int {
        ctx.apply(
                ::bindLabelComponents,
                ::bindPluginStatus,
                ::bindDependencies
        )
        return 0
    }

    private fun bindDependencies(ctx: Context) {
        val javaSources = mutableListOf<String>()
        val allSources = mutableListOf<String>()

        val sourcePool = with(mutableListOf<String>()) {
            ctx.flags.source?.also {
                check(it.isNotEmpty())
                addAll(it)
            }
            addAll(Metas.UNPACKED_SOURCES[ctx] ?: emptyList())
            toList()
        }

        if(sourcePool.isEmpty()) {
            throw RuntimeException("no compilable sources found")
        }

        for (src in sourcePool) {
            when {
                src.endsWith(".java") -> {
                    javaSources.add(src)
                    allSources.add(src)
                }
                src.endsWith(".kt") -> allSources.add(src)
                else -> throw RuntimeException("unrecognised file type: $src")
            }
        }
        CompileDependencies[ctx] = CompileDependencies(
                classpath = ctx.flags.classpath.toSet(),
                directDependencies = ctx.flags.directDependencies,
                indirectDependencies = ctx.flags.indirectDependencies,
                javaSources = javaSources,
                allSources = allSources
        )
    }

    private fun bindPluginStatus(ctx: Context) {
        CompilePluginConfig[ctx] = ctx.flags.plugins?.let {
            PluginArgs.from(ctx)?.let {
                CompilePluginConfig(hasAnnotationProcessors = true, args = it.toTypedArray())
            }
        }
    }

    /**
     * parses the label, sets up the meta elements and returns the target part.
     */
    private fun bindLabelComponents(ctx: Context) {
        val parts = ctx.flags.label.split(":")
        require(parts.size == 2) { "the label ${ctx.flags.label} is invalid" }
        Metas.PKG[ctx] = parts[0]
        Metas.TARGET[ctx] = parts[1]
    }
}
