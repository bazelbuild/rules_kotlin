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

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.bazel.kotlin.builder.CompilationStatusException
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.model.KotlinModel.BuilderCommand

@ImplementedBy(DefaultJavaCompiler::class)
interface JavaCompiler {
    fun compile(command: BuilderCommand)
}

private class DefaultJavaCompiler @Inject constructor(
    val javacInvoker: KotlinToolchain.JavacInvoker
) : JavaCompiler {
    override fun compile(command: BuilderCommand) {
        val i = command.inputs
        val d = command.directories
        if (i.javaSourcesList.isNotEmpty() || i.generatedJavaSourcesList.isNotEmpty()) {
            val args = mutableListOf(
                "-cp", "${d.classes}/:${d.temp}/:${i.joinedClasspath}",
                "-d", d.classes
            ).let {
                it.addAll(
                    // Kotlin takes care of annotation processing.
                    "-proc:none",
                    // Disable option linting, it will complain about the source.
                    "-Xlint:-options",
                    "-source", command.info.toolchainInfo.jvm.jvmTarget,
                    "-target", command.info.toolchainInfo.jvm.jvmTarget
                )
                it.addAll(i.javaSourcesList)
                it.addAll(i.generatedJavaSourcesList)
                it.toTypedArray()
            }
            javacInvoker.compile(args).takeIf { it != 0 }?.also { throw CompilationStatusException("javac failed",it) }
        }
    }
}