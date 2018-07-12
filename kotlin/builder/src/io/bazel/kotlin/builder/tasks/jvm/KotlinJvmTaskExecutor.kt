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

import com.google.common.base.Stopwatch
import com.google.inject.Inject
import com.google.inject.Singleton
import io.bazel.kotlin.builder.CompilationStatusException
import io.bazel.kotlin.builder.utils.expandWithGeneratedSources
import io.bazel.kotlin.model.KotlinModel.CompilationTask
import java.io.File
import java.util.concurrent.TimeUnit


@Singleton
class KotlinJvmTaskExecutor @Inject internal constructor(
    private val kotlinCompiler: KotlinCompiler,
    private val outputSink: KotlinCompilerOutputSink,
    private val javaCompiler: JavaCompiler,
    private val jDepsGenerator: JDepsGenerator,
    private val outputJarCreator: OutputJarCreator
) {
    class Result(val timings: List<String>, val command: CompilationTask)

    fun compile(command: CompilationTask): Result {
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

    private fun runAnnotationProcessors(command: CompilationTask): CompilationTask =
        try {
            if (command.info.plugins.annotationProcessorsList.isNotEmpty()) {
                kotlinCompiler.runAnnotationProcessor(command)
                File(command.directories.generatedSources).walkTopDown()
                    .filter { it.isFile }
                    .map { it.path }
                    .iterator()
                    .let { command.expandWithGeneratedSources(command, it) }
            } else {
                command
            }
        } catch (ex: CompilationStatusException) {
            ex.lines.also(outputSink::deliver)
            throw ex
        }

    private fun compileClasses(context: Context, command: CompilationTask) {
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
