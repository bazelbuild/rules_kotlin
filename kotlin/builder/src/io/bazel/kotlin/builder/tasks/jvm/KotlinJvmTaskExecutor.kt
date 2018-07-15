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
import io.bazel.kotlin.builder.utils.expandWithSources
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import io.bazel.kotlin.model.KotlinModel.CompilationTask
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KotlinJvmTaskExecutor @Inject internal constructor(
    private val kotlinCompiler: KotlinJvmCompiler,
    private val outputSink: KotlinCompilerOutputSink,
    private val javaCompiler: JavaCompiler,
    private val jDepsGenerator: JDepsGenerator,
    private val outputJarCreator: OutputJarCreator
) {
    @Suppress("unused")
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
        produceSourceJar(commandWithApSources)
        context.execute("generate jdeps") {
            jDepsGenerator.generateJDeps(commandWithApSources)
        }
        return Result(context.timings, commandWithApSources)
    }

    private fun produceSourceJar(command: CompilationTask) {
        Paths.get(command.outputs.srcjar).also { sourceJarPath ->
            Files.createFile(sourceJarPath)
            SourceJarCreator(
                sourceJarPath
            ).also { creator ->
                // This check asserts that source jars were unpacked.
                check(
                    command.inputs.sourceJarsList.isEmpty() ||
                            Files.exists(Paths.get(command.directories.temp).resolve("_srcjars"))
                )
                listOf(
                    // Any (input) source jars should already have been expanded so do not add them here.
                    command.inputs.javaSourcesList.stream(),
                    command.inputs.kotlinSourcesList.stream()
                ).stream()
                    .flatMap { it.map { Paths.get(it) } }
                    .also { creator.addSources(it) }
                creator.execute()
            }
        }
    }

    private fun runAnnotationProcessors(command: CompilationTask): CompilationTask =
        try {
            if (command.info.plugins.annotationProcessorsList.isNotEmpty()) {
                kotlinCompiler.runAnnotationProcessor(command)
                File(command.directories.generatedSources).walkTopDown()
                    .filter { it.isFile }
                    .map { it.path }
                    .iterator()
                    .let { command.expandWithSources(it) }
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
        inline fun <T> execute(name: String, task: () -> T): T {
            val start = System.currentTimeMillis()
            return try {
                task()
            } finally {
                val stop = System.currentTimeMillis()
                timings += "$name: ${stop - start} ms"
            }
        }
    }
}
