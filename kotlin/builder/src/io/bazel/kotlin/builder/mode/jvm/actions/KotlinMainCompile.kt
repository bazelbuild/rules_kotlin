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
import io.bazel.kotlin.builder.mode.jvm.utils.KotlinCompilerOutputProcessor
import io.bazel.kotlin.builder.model.CompileDependencies
import io.bazel.kotlin.builder.model.CompilePluginConfig
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.builder.utils.annotationProcessingGeneratedJavaSources
import io.bazel.kotlin.builder.utils.moduleName

// The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
// invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
class KotlinMainCompile(toolchain: KotlinToolchain) : BuildAction("compile kotlin classes", toolchain) {
    companion object {
        val Result = CompileResult.Meta("kotlin_compile_result")
    }

    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private fun setupCompileContext(ctx: Context): MutableList<String> {
        val args = mutableListOf<String>()

        args.addAll(
                "-cp", CompileDependencies[ctx].classPathString,
                "-api-version", ctx.flags.kotlinApiVersion,
                "-language-version", ctx.flags.kotlinLanguageVersion,
                "-jvm-target", ctx.flags.kotlinJvmTarget
        )

        args
                .addAll("-module-name", ctx.moduleName)
                .addAll("-d", ctx.flags.classDir.value.toString())

        ctx.flags.kotlinPassthroughFlags?.takeIf { it.isNotBlank() }?.also { args.addAll(it.split(" ")) }

        return args
    }

    override fun invoke(ctx: Context): Int {
        val commonArgs = setupCompileContext(ctx)
        val sources = CompileDependencies[ctx].allSources
        val pluginStatus = CompilePluginConfig[ctx]

        // run a kapt generation phase if needed.
        if (pluginStatus.hasAnnotationProcessors) {
            invokeCompilePhase(
                    args = mutableListOf(*commonArgs.toTypedArray()).let {
                        it.addAll(pluginStatus.args)
                        it.addAll(sources)
                        it.toTypedArray()
                    },
                    onNonTeminalExitCode = { outputProcessors, exitCode ->
                        outputProcessors.process()
                        exitCode
                    }
            ).takeIf { it != 0 }?.also { return it }
        }
        return invokeCompilePhase(
                args = commonArgs.let { args ->
                    args.addAll(sources)
                    ctx.annotationProcessingGeneratedJavaSources()?.also { args.addAll(it) }
                    args.toTypedArray()
                },
                onNonTeminalExitCode = { outputProcessor, exitCode ->
                    // give javac a chance to process the java sources.
                    Result[ctx] = CompileResult.deferred(exitCode) { _ ->
                        outputProcessor.process()
                        exitCode
                    }
                    0
                }
        )
    }

    private fun invokeCompilePhase(args: Array<String>, onNonTeminalExitCode: (KotlinCompilerOutputProcessor, Int) -> Int): Int {
        val outputProcessor = KotlinCompilerOutputProcessor.ForKotlinC(System.out)

        val exitCode = try {
            toolchain.kotlinCompiler.compile(args, outputProcessor.collector)
        } catch (ex: Exception) {
            outputProcessor.process()
            throw ex
        }

        if (exitCode < 2) {
            // 1 is a standard compilation error
            // 2 is an internal error
            // 3 is the script execution error
            return onNonTeminalExitCode(outputProcessor, exitCode)
        } else {
            outputProcessor.process()
            throw RuntimeException("KotlinMainCompile returned terminal error code: $exitCode")
        }
    }
}
