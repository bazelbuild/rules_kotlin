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

import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.bazel.kotlin.builder.KotlinToolchain.Companion.NO_ARGS
import io.bazel.kotlin.builder.utils.executeAndAwait
import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.resolveVerifiedToAbsoluteString
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter

internal object KotlinToolchainModule : AbstractModule() {
    @Provides
    fun jarToolInvoker(toolchain: KotlinToolchain): KotlinToolchain.JarToolInvoker =
        object : KotlinToolchain.JarToolInvoker {
            override fun invoke(args: List<String>, directory: File?) {
                val jarTool = toolchain.javaHome.resolveVerifiedToAbsoluteString("bin", "jar")
                val command = mutableListOf(jarTool).also { it.addAll(args) }
                executeAndAwait(10, directory, command).takeIf { it != 0 }?.also {
                    throw CompilationStatusException("error running jar command ${command.joinToString(" ")}", it)
                }
            }
        }

    @Provides
    fun javacInvoker(toolchain: KotlinToolchain): KotlinToolchain.JavacInvoker = object : KotlinToolchain.JavacInvoker {
        val c = toolchain.classLoader.loadClass("com.sun.tools.javac.Main")
        val m = c.getMethod("compile", Array<String>::class.java)
        val mPw = c.getMethod("compile", Array<String>::class.java, PrintWriter::class.java)
        override fun compile(args: Array<String>) = m.invoke(c, args) as Int
        override fun compile(args: Array<String>, out: PrintWriter) = mPw.invoke(c, args, out) as Int
    }

    @Provides
    fun jdepsInvoker(toolchain: KotlinToolchain): KotlinToolchain.JDepsInvoker = object : KotlinToolchain.JDepsInvoker {
        val clazz = toolchain.classLoader.loadClass("com.sun.tools.jdeps.Main")
        val method = clazz.getMethod("run", Array<String>::class.java, PrintWriter::class.java)
        override fun run(args: Array<String>, out: PrintWriter): Int = method.invoke(clazz, args, out) as Int
    }

    @Provides
    fun kotlincInvoker(toolchain: KotlinToolchain): KotlinToolchain.KotlincInvoker =
        object : KotlinToolchain.KotlincInvoker {
            val compilerClass = toolchain.classLoader.loadClass("io.bazel.kotlin.compiler.BazelK2JVMCompiler")
            val exitCodeClass = toolchain.classLoader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode")

            val compiler = compilerClass.getConstructor().newInstance()
            val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
            val getCodeMethod = exitCodeClass.getMethod("getCode")


            override fun compile(args: Array<String>, out: PrintStream): Int {
                val exitCodeInstance = execMethod.invoke(compiler, out, args)
                return getCodeMethod.invoke(exitCodeInstance, *NO_ARGS) as Int
            }
        }

    @Provides
    @KotlinToolchain.CompilerPlugin.Kapt3
    fun provideKapt3(toolchain: KotlinToolchain): KotlinToolchain.CompilerPlugin =
        KotlinToolchain.CompilerPlugin(
            toolchain.kotlinHome.resolveVerified("lib", "kotlin-annotation-processing.jar").absolutePath,
            "org.jetbrains.kotlin.kapt3"
        )

    @Provides
    fun provideBazelWorker(kotlinBuilder: KotlinBuilder, output: PrintStream): BazelWorker =
        BazelWorker(kotlinBuilder, output, "KotlinCompile")
}