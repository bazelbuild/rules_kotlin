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
import io.bazel.ruleskotlin.workers.CompileResult
import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.KotlinToolchain
import io.bazel.ruleskotlin.workers.utils.annotationProcessingGeneratedJavaSources
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.Flags
import io.bazel.ruleskotlin.workers.model.Metas
import io.bazel.ruleskotlin.workers.utils.executeAndAwait

/**
 * Simple java compile action that invokes javac directly and simply.
 */
class JavaMainCompile(toolchain: KotlinToolchain) : BuildAction("compile java classes", toolchain) {
    companion object {
        val Result = CompileResult.Meta("javac_compile_result")
    }

    override fun invoke(ctx: Context): Int {
        val javaSources = Metas.JAVA_SOURCES.mustGet(ctx)
        val classpath = Flags.CLASSPATH[ctx]
        val additionalJavaSources = ctx.annotationProcessingGeneratedJavaSources()?.toList() ?: emptyList()

        if (javaSources.isNotEmpty() || additionalJavaSources.isNotEmpty()) {
            val classesDirectory = CompileDirectories[ctx].classes
            val incrementalData = CompileDirectories[ctx].annotationProcessingIncrementalData

            val args = mutableListOf(toolchain.JAVAC_PATH, "-cp", "$classesDirectory/:$incrementalData/:$classpath", "-d", classesDirectory.toString()).also {
                // Kotlin takes care of annotation processing.
                it.add("-proc:none")
                it.addAll(javaSources)
                it.addAll(additionalJavaSources)
            }
            Result.runAndBind(ctx) { executeAndAwait(30, args) }
        }
        return 0
    }
}