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
import io.bazel.kotlin.builder.CompileResult
import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.model.Metas
import io.bazel.kotlin.builder.utils.annotationProcessingGeneratedJavaSources
import io.bazel.kotlin.builder.utils.executeAndAwait

/**
 * Simple java compile action that invokes javac directly and simply.
 */
class JavaMainCompile(toolchain: KotlinToolchain) : BuildAction("compile java classes", toolchain) {
    companion object {
        val Result = CompileResult.Meta("javac_compile_result")
    }

    override fun invoke(ctx: Context): Int {
        val javaSources = Metas.JAVA_SOURCES.mustGet(ctx)

        val additionalJavaSources = ctx.annotationProcessingGeneratedJavaSources()?.toList() ?: emptyList()

        if (javaSources.isNotEmpty() || additionalJavaSources.isNotEmpty()) {
            val classesDirectory = ctx.flags.classDir.value.toString()
            val incrementalData = ctx.flags.tempDirPath.value.toString()

            val args = mutableListOf(toolchain.JAVAC_PATH, "-cp", "$classesDirectory/:$incrementalData/:${ctx.flags.classpath.joinToString(":")}", "-d", classesDirectory).also {
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