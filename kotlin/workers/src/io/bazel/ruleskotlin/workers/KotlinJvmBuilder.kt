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
package io.bazel.ruleskotlin.workers


import io.bazel.ruleskotlin.workers.compilers.jvm.actions.*
import io.bazel.ruleskotlin.workers.model.Flags
import java.io.IOException

/**
 * Bazel Kotlin Compiler worker.
 */
object KotlinJvmBuilder : CommandLineProgram.Base(
        flags = Flags::class.flagsByName()
) {
    private val toolchain: KotlinToolchain = try {
        KotlinToolchain()
    } catch (e: IOException) {
        throw RuntimeException("could not initialize toolchain", e)
    }

    private val compileActions: List<BuildAction> = listOf(
            Initialize(toolchain),
            KotlinMainCompile(toolchain),
            JavaMainCompile(toolchain),
            ProcessCompileResult(toolchain),
            CreateOutputJar(toolchain),
            GenerateJdepsFile(toolchain)
    )

    override fun toolchain(ctx: Context): KotlinToolchain = toolchain
    override fun actions(toolchain: KotlinToolchain, ctx: Context): List<BuildAction> = compileActions

    @JvmStatic
    fun main(args: Array<String>) {
        val kotlinCompilerBazelWorker = BazelWorker(
                this,
                System.err,
                "KotlinCompile"
        )
        System.exit(kotlinCompilerBazelWorker.apply(args.toList()))
    }
}
