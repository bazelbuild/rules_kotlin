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
package io.bazel.kotlin.builder.mode.jvm

import com.google.common.base.Stopwatch
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import io.bazel.kotlin.builder.BuildCommandBuilder
import io.bazel.kotlin.builder.CompilationStatusException
import io.bazel.kotlin.builder.ToolException
import io.bazel.kotlin.builder.mode.jvm.KotlinJvmCompilationExecutor.Result
import io.bazel.kotlin.builder.mode.jvm.actions.JDepsGenerator
import io.bazel.kotlin.builder.mode.jvm.actions.JavaCompiler
import io.bazel.kotlin.builder.mode.jvm.actions.KotlinCompiler
import io.bazel.kotlin.builder.mode.jvm.actions.OutputJarCreator
import io.bazel.kotlin.builder.mode.jvm.utils.KotlinCompilerOutputSink
import io.bazel.kotlin.model.KotlinModel.BuilderCommand
import java.io.File
import java.util.concurrent.TimeUnit

@ImplementedBy(DefaultKotlinJvmCompilationExecutor::class)
interface KotlinJvmCompilationExecutor {
    class Result(val timings: List<String>, val command: BuilderCommand)

    fun compile(command: BuilderCommand): Result
}

@Singleton
private class DefaultKotlinJvmCompilationExecutor @Inject constructor(
    private val commandBuilder: BuildCommandBuilder,
    private val kotlinCompiler: KotlinCompiler,
    private val outputSink: KotlinCompilerOutputSink,
    private val javaCompiler: JavaCompiler,
    private val jDepsGenerator: JDepsGenerator,
    private val outputJarCreator: OutputJarCreator
) : KotlinJvmCompilationExecutor {
    override fun compile(command: BuilderCommand): Result {
        val context = Context()
        val commandWithApSources = context.execute("kapt") {
            runAnnotationProcessors(command)
        }
        compileClasses(context, commandWithApSources)
        context.execute("create jar") {
            outputJarCreator.createOutputJar(commandWithApSources)
        }
        context.execute("generate jdeps") {
            jDepsGenerator.generateJDeps(commandWithApSources)
        }
        return Result(context.timings, commandWithApSources)
    }

    private fun runAnnotationProcessors(command: BuilderCommand): BuilderCommand =
        if (command.info.plugins.annotationProcessorsList.isNotEmpty()) {
            kotlinCompiler.runAnnotationProcessor(command)
            File(command.outputs.sourceGenDir).walkTopDown()
                .filter { it.isFile }
                .map { it.path }
                .iterator()
                .let { commandBuilder.withGeneratedSources(command, it) }
        } else {
            command
        }

    private fun compileClasses(context: Context, command: BuilderCommand) {
        var kotlinError: CompilationStatusException? = null
        var result: List<String>? = null
        context.execute("kotlinc") {
            result = try {
                kotlinCompiler.compile(command)
            } catch (ex: CompilationStatusException) {
                kotlinError = ex
                ex.lines
            }
        }
        try {
            context.execute("javac") {
                javaCompiler.compile(command)
            }
        } finally {
            checkNotNull(result).also(outputSink::deliver)
            kotlinError?.also { throw it }
        }
    }

    internal class Context {
        val timings = mutableListOf<String>()
        val sw: Stopwatch = Stopwatch.createUnstarted()
        inline fun <T> execute(name: String, task: () -> T): T {
            sw.start()
            return try {
                task()
            } finally {
                sw.stop()
                timings += "$name: ${sw.elapsed(TimeUnit.MILLISECONDS)} ms"
                sw.reset()
            }
        }
    }
}
