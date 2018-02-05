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
import io.bazel.ruleskotlin.workers.model.Metas
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.KotlinCompilerOutputProcessor
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.Flags
import java.util.*

/**
 * Either compiles to a jar directly or when performing mixed-mode-compilation compiles to a temp directory first.
 *
 *
 * Mixed-Mode:
 *
 *
 * The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
 * invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
 */
class KotlinMainCompile(toolchain: KotlinToolchain) : BuildAction("compile kotlin classes", toolchain) {
    companion object {
        /**
         * Default fields that are directly mappable to kotlin compiler args.
         */
        private val COMPILE_MAPPED_FLAGS = arrayOf(
                Flags.CLASSPATH,
                Flags.KOTLIN_API_VERSION,
                Flags.KOTLIN_LANGUAGE_VERSION,
                Flags.KOTLIN_JVM_TARGET)

        val Result = CompileResult.Meta("kotlin_compile_result")
    }

    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private fun setupCompileContext(ctx: Context): Array<String> {
        val args = mutableListOf<String>()
        val compileDirectories = CompileDirectories[ctx]

        ctx.copyOfFlags(*COMPILE_MAPPED_FLAGS).forEach { field, arg ->
            args.add(field.kotlinFlag!!); args.add(arg)
        }

        Collections.addAll(args, "-kotlin-home", KotlinToolchain.KOTLIN_HOME.toString())
        Collections.addAll(args, "-d", compileDirectories.classes)


        args.addAll(Metas.ALL_SOURCES.mustGet(ctx))
        return args.toTypedArray()
    }

    override fun invoke(ctx: Context): Int {
        val outputProcessor = KotlinCompilerOutputProcessor.ForKotlinC(System.out)

        val exitCode = try {
            toolchain.kotlinCompiler.compile(setupCompileContext(ctx), outputProcessor.collector)
        } catch (ex: Exception) {
            outputProcessor.process()
            throw ex
        }

        if (exitCode < 2) {
            // 1 is a standard compilation error
            // 2 is an internal error
            // 3 is the script execution error

            // give javac a chance to process the java sources.
            Result[ctx] = CompileResult.deferred(exitCode) { _ ->
                outputProcessor.process()
                exitCode
            }
            return 0
        } else {
            outputProcessor.process()
            throw RuntimeException("KotlinMainCompile returned terminal error code: $exitCode")
        }
    }
}
