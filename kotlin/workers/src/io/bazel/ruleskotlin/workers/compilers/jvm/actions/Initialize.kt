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


import io.bazel.ruleskotlin.workers.*
import io.bazel.ruleskotlin.workers.compilers.jvm.Metas

import io.bazel.ruleskotlin.workers.utils.purgeDirectory

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Collections

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
        val javaSources = ArrayList<String>()
        val allSources = ArrayList<String>()
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
        Metas.JAVA_SOURCES.bind(ctx, Collections.unmodifiableList(javaSources))
        Metas.ALL_SOURCES.bind(ctx, Collections.unmodifiableList(allSources))
    }

    private fun initializeAndBindBindDirectories(ctx: Context) {
        val outputBase: Path

        try {
            outputBase = Files.createDirectories(Paths.get(checkNotNull(Flags.COMPILER_OUTPUT_BASE[ctx])))
        } catch (e: IOException) {
            throw RuntimeException("could not create compiler output base", e)
        }

        try {
            outputBase.purgeDirectory()
        } catch (e: IOException) {
            throw RuntimeException("could not purge output directory", e)
        }

        createAndBindComponentDirectory(ctx, outputBase, Metas.CLASSES_DIRECTORY, "_classes")
    }

    private fun createAndBindComponentDirectory(ctx: Context, outputBase: Path, key: Meta<Path>, component: String) {
        try {
            key.bind(ctx, Files.createDirectories(outputBase.resolve(component)))
        } catch (e: IOException) {
            throw RuntimeException("could not create subdirectory for component " + component, e)
        }

    }

    /**
     * parses the label, sets up the meta elements and returns the target part.
     */
    private fun bindLabelComponents(ctx: Context) {
        val label = requireNotNull(Flags.LABEL[ctx])
        val parts = label.split(":")
        require(parts.size == 2) { "the label $label is invalid" }
        Metas.PKG.bind(ctx, parts[0])
        Metas.TARGET.bind(ctx, parts[1])
    }
}
