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
package io.bazel.kotlin.builder


import io.bazel.kotlin.builder.mode.jvm.actions.*
import java.io.IOException

/**
 * Bazel Kotlin Compiler worker.
 */
object KotlinBuilder : CommandLineProgram.Base() {
    override val toolchain: KotlinToolchain = try {
        KotlinToolchain()
    } catch (e: IOException) {
        throw RuntimeException("could not initialize toolchain", e)
    }

    private val compileActions: List<BuildAction> = listOf(
            UnpackSourceJars(toolchain),
            Initialize(toolchain),
            KotlinMainCompile(toolchain),
            JavaMainCompile(toolchain),
            ProcessCompileResult(toolchain),
            CreateOutputJar(toolchain),
            GenerateJdepsFile(toolchain)
    )

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
