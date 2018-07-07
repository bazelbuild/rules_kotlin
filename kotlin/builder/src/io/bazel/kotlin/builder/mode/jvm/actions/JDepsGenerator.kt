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
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.bazel.kotlin.builder.CompilationException
import io.bazel.kotlin.builder.CompilationStatusException
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.mode.jvm.utils.JdepsParser
import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.rootCause
import io.bazel.kotlin.model.KotlinModel
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

@ImplementedBy(DefaultJDepsGenerator::class)
interface JDepsGenerator {
    fun generateJDeps(command: KotlinModel.BuilderCommand)
}

private class DefaultJDepsGenerator @Inject constructor(
    toolchain: KotlinToolchain,
    val invoker: KotlinToolchain.JDepsInvoker
) : JDepsGenerator {
    private val isKotlinImplicit = JdepsParser.pathSuffixMatchingPredicate(
        toolchain.kotlinHome.resolveVerified("lib").toPath(), *toolchain.kotlinStandardLibraries.toTypedArray())
    override fun generateJDeps(command: KotlinModel.BuilderCommand) {
        val jdepsContent =
            if (command.inputs.classpathList.isEmpty()) {
                Deps.Dependencies.newBuilder().let {
                    it.ruleLabel = command.info.label
                    it.build()
                }
            } else {
                ByteArrayOutputStream().use { out ->
                    PrintWriter(out).use { writer ->
                        val res = invoker.run(
                            arrayOf(
                                "-cp", command.inputs.joinedClasspath,
                                command.outputs.jdeps),
                            writer)
                        out.toByteArray().inputStream().bufferedReader().readLines().let {
                            if (res != 0) {
                                throw CompilationStatusException("could not run jdeps tool", res, it)
                            }
                            try {
                                JdepsParser.parse(
                                    command.info.label,
                                    command.outputs.jdeps,
                                    command.inputs.joinedClasspath,
                                    it,
                                    isKotlinImplicit
                                )
                            } catch (e: Exception) {
                                throw CompilationException("error reading or parsing jdeps file", e.rootCause)
                            }
                        }
                    }
                }
            }
        Paths.get(command.outputs.jdeps).also {
            Files.deleteIfExists(it)
            FileOutputStream(Files.createFile(it).toFile()).use {
                jdepsContent.writeTo(it)
            }
        }
    }
}