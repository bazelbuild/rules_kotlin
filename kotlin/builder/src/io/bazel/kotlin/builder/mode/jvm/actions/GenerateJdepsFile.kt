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

import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.BuildAction
import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.mode.jvm.utils.JdepsParser
import io.bazel.kotlin.builder.model.CompileDependencies
import io.bazel.kotlin.builder.model.Metas
import io.bazel.kotlin.builder.utils.executeAndWaitOutput
import io.bazel.kotlin.builder.utils.rootCause
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


class GenerateJdepsFile(toolchain: KotlinToolchain) : BuildAction("generate jdeps", toolchain) {
    private val isKotlinImplicit = JdepsParser.pathSuffixMatchingPredicate(toolchain.KOTLIN_LIB_DIR, *toolchain.KOTLIN_STD_LIBS)

    override fun invoke(ctx: Context): Int {
        val jdepsContent: Deps.Dependencies
        val classpath = CompileDependencies[ctx].classPathString
        try {
            jdepsContent = executeAndWaitOutput(10, toolchain.JDEPS_PATH, "-cp", classpath, ctx.flags.outputClassJar).let {
                JdepsParser.parse(
                        ctx.flags.label,
                        ctx.flags.outputClassJar,
                        classpath,
                        it.stream(),
                        isKotlinImplicit
                )
            }
            Metas.JDEPS[ctx] = jdepsContent
        } catch (e: Exception) {
            throw RuntimeException("error reading or parsing jdeps file", e.rootCause)
        }

        try {
            Paths.get(ctx.flags.outputJdeps).also {
                Files.deleteIfExists(it)
                FileOutputStream(Files.createFile(it).toFile()).use {
                    jdepsContent.writeTo(it)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("error writing out jdeps file", e.rootCause)
        }
        return 0
    }
}
