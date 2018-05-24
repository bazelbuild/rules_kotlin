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
import io.bazel.kotlin.model.KotlinModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@ImplementedBy(DefaultKotlinCompiler::class)
interface KotlinCompiler {
    fun runAnnotationProcessor(command: KotlinModel.BuilderCommand): List<String>
    fun compile(command: KotlinModel.BuilderCommand): List<String>
}

// The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
// invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
//
// 1 is a standard compilation error
// 2 is an internal error
// 3 is the script execution error
private class DefaultKotlinCompiler @Inject constructor(
    val compiler: KotlinToolchain.KotlincInvoker
) : KotlinCompiler {
    override fun runAnnotationProcessor(command: KotlinModel.BuilderCommand): List<String> {
        check(command.info.plugins.annotationProcessorsList.isNotEmpty()) {
            "method called without annotation processors"
        }
        return setupCompileContext(command).also {
            it.addAll(command.info.encodedPluginDescriptorsList)
            it.addAll(command.inputs.kotlinSourcesList)
            it.addAll(command.inputs.javaSourcesList)
        }.let { invokeCompilePhase(it) }
    }

    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private fun setupCompileContext(command: KotlinModel.BuilderCommand): MutableList<String> {
        val args = mutableListOf<String>()

        // use -- for flags not meant for the kotlin compiler
        args.addAll(
            "-cp", command.inputs.joinedClasspath,
            "-api-version", command.info.toolchainInfo.common.apiVersion,
            "-language-version", command.info.toolchainInfo.common.languageVersion,
            "-jvm-target", command.info.toolchainInfo.jvm.jvmTarget,
            // https://github.com/bazelbuild/rules_kotlin/issues/69: remove once jetbrains adds a flag for it.
            "--friend-paths", command.info.friendPathsList.joinToString(":")
        )

        args
            .addAll("-module-name", command.info.kotlinModuleName)
            .addAll("-d", command.outputs.classDirectory)

        command.info.passthroughFlags?.takeIf { it.isNotBlank() }?.also { args.addAll(it.split(" ")) }
        return args
    }

    override fun compile(command: KotlinModel.BuilderCommand): List<String> =
        with(setupCompileContext(command)) {
            addAll(command.inputs.javaSourcesList)
            addAll(command.inputs.generatedJavaSourcesList)
            addAll(command.inputs.kotlinSourcesList)
            addAll(command.inputs.generatedKotlinSourcesList)
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