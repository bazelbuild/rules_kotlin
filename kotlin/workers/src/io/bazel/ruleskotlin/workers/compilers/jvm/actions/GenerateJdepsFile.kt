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

import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.ruleskotlin.workers.BuildAction
import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.KotlinToolchain
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.JdepsParser
import io.bazel.ruleskotlin.workers.model.Flags.CLASSPATH
import io.bazel.ruleskotlin.workers.model.Flags.LABEL
import io.bazel.ruleskotlin.workers.model.Flags.OUTPUT_CLASSJAR
import io.bazel.ruleskotlin.workers.model.Flags.OUTPUT_JDEPS
import io.bazel.ruleskotlin.workers.utils.executeAndWaitOutput
import io.bazel.ruleskotlin.workers.utils.rootCause
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


class GenerateJdepsFile(toolchain: KotlinToolchain) : BuildAction("generate jdeps", toolchain) {
    private val isKotlinImplicit = JdepsParser.pathSuffixMatchingPredicate(toolchain.KOTLIN_LIB_DIR, *toolchain.KOTLIN_STD_LIBS)

    override fun invoke(ctx: Context): Int {
        val classJar = OUTPUT_CLASSJAR[ctx]
        val classPath = CLASSPATH[ctx]
        val output = OUTPUT_JDEPS[ctx]
        val label = LABEL[ctx]

        val jdepsContent: Deps.Dependencies

        try {
            jdepsContent = executeAndWaitOutput(10, toolchain.JDEPS_PATH, "-cp", classPath, classJar).let {
                JdepsParser.parse(label, classJar, classPath, it.stream(), isKotlinImplicit)
            }
        } catch (e: Exception) {
            throw RuntimeException("error reading or parsing jdeps file", e.rootCause)
        }

        try {
            Paths.get(output).also {
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
