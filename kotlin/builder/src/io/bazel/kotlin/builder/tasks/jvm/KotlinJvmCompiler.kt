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
package io.bazel.kotlin.builder.tasks.jvm

import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.KotlinCompilerPluginArgsEncoder
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.builder.utils.joinedClasspath
import io.bazel.kotlin.model.JvmCompilationTask
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.inject.Inject
import javax.inject.Singleton


// The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
// invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
//
// 1 is a standard compilation error
// 2 is an internal error
// 3 is the script execution error
@Singleton
internal class KotlinJvmCompiler @Inject constructor(
    private val compiler: KotlinToolchain.KotlincInvoker,
    private val pluginArgsEncoder: KotlinCompilerPluginArgsEncoder
) {
    fun runAnnotationProcessor(command: JvmCompilationTask): List<String> {
        check(command.info.plugins.annotationProcessorsList.isNotEmpty()) {
            "method called without annotation processors"
        }
        return getCommonArgs(command).also {
            it.addAll(pluginArgsEncoder.encode(command))
            it.addAll(command.inputs.kotlinSourcesList)
            it.addAll(command.inputs.javaSourcesList)
        }.let(::invokeCompilePhase)
    }

    /**
     * Return a list with the common arguments.
     */
    private fun getCommonArgs(command: JvmCompilationTask): MutableList<String> {
        val args = mutableListOf<String>()

        // use -- for flags not meant for the kotlin compiler
        args.addAll(
            "-cp", command.inputs.joinedClasspath,
            "-api-version", command.info.toolchainInfo.common.apiVersion,
            "-language-version", command.info.toolchainInfo.common.languageVersion,
            "-jvm-target", command.info.toolchainInfo.jvm.jvmTarget,
            // https://github.com/bazelbuild/rules_kotlin/issues/69: remove once jetbrains adds a flag for it.
            "--friend-paths", command.info.friendPathsList.joinToString(File.pathSeparator)
        )

        args
            .addAll("-module-name", command.info.moduleName)
            .addAll("-d", command.directories.classes)

        command.info.passthroughFlags?.takeIf { it.isNotBlank() }?.also { args.addAll(it.split(" ")) }
        return args
    }

    fun compile(command: JvmCompilationTask): List<String> =
        with(getCommonArgs(command)) {
            addAll(command.inputs.javaSourcesList)
            addAll(command.inputs.kotlinSourcesList)
            invokeCompilePhase(this)
        }

    private fun invokeCompilePhase(args: List<String>): List<String> {
        val outputStream = ByteArrayOutputStream()
        val ps = PrintStream(outputStream)
        val result = compiler.compile(args.toTypedArray(), ps)
        val output = ByteArrayInputStream(outputStream.toByteArray()).bufferedReader().readLines()
        if (result != 0) {
            throw CompilationStatusException("compile phase failed", result, output)
        } else {
            return output
        }
    }
}