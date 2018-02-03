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
package io.bazel.ruleskotlin.workers.compilers.jvm


import io.bazel.ruleskotlin.workers.*
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.*

import java.io.IOException

/**
 * Bazel Kotlin Compiler worker.
 */
class KotlinJvmBuilder private constructor() : CommandLineProgram {
    private val compileActions: Array<BuildAction>

    init {
        val toolchain: KotlinToolchain
        try {
            toolchain = KotlinToolchain()
        } catch (e: IOException) {
            throw RuntimeException("could not initialize toolchain", e)
        }

        compileActions = arrayOf(
                Initialize(toolchain),
                KotlinMainCompile(toolchain),
                JavaMainCompile(toolchain),
                ProcessCompileResult(toolchain),
                CreateOutputJar(toolchain),
                GenerateJdepsFile(toolchain)
        )
    }

    override fun apply(args: List<String>): Int {
        val ctx = Context.from(args)
        var exitCode = 0
        for (action in compileActions) {
            exitCode = action(ctx)
            if (exitCode != 0)
                break
        }
        return exitCode
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val kotlinBuilder = KotlinJvmBuilder()
            val kotlinCompilerBazelWorker = BazelWorker(
                    kotlinBuilder,
                    System.err,
                    "KotlinCompile"
            )
            System.exit(kotlinCompilerBazelWorker.apply(args.toList()))
        }
    }
}
