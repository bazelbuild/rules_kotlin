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
package io.bazel.kotlin.builder.tasks

import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.utils.ArgMaps
import io.bazel.kotlin.builder.utils.IS_JVM_SOURCE_FILE
import io.bazel.kotlin.builder.utils.ensureDirectories
import io.bazel.kotlin.builder.utils.expandWithSources
import io.bazel.kotlin.builder.utils.jars.SourceJarExtractor
import io.bazel.kotlin.model.KotlinModel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("MemberVisibilityCanBePrivate")
class KotlinBuilder @Inject internal constructor(
    private val taskBuilder: TaskBuilder,
    private val jvmTaskExecutor: KotlinJvmTaskExecutor
) : CommandLineProgram {
    companion object {
        // regex that matches the regular bazel param file naming convention.
        private val STANDARD_FLAGFILE_RE = Pattern.compile(""".*.jar-\d+.params$""").toRegex()
    }

    fun execute(args: List<String>): Int {
        check(args.isNotEmpty() && args[0].startsWith("--flagfile=")) { "no flag file supplied" }
        val flagFile = args[0].replace("--flagfile=", "")
        val flagFilePath = Paths.get(flagFile)
        check(flagFilePath.toFile().exists()) { "flagfile $flagFile does not exist" }
        val task = when {
            STANDARD_FLAGFILE_RE.matches(flagFile) -> {
                Files.readAllLines(flagFilePath, UTF_8).let { loadedFlags ->
                    ArgMaps.from(loadedFlags).let {
                        taskBuilder.fromInput(it)
                    }
                }
            }
            else -> throw IllegalStateException("unknown flag file format for $flagFile")
        }
        return execute(task)
    }

    fun execute(command: KotlinModel.CompilationTask): Int {
        ensureDirectories(
            command.directories.classes,
            command.directories.temp,
            command.directories.generatedSources,
            command.directories.generatedClasses
        )
        val updatedCommand = expandWithSourceJarSources(command)
        return try {
            jvmTaskExecutor.compile(updatedCommand)
            0
        } catch (ex: CompilationStatusException) {
            ex.status
        }
    }

    /**
     * If any sourcejars were provided expand the jars sources and create a new [KotlinModel.CompilationTask] with the
     * Java and Kotlin sources merged in.
     */
    private fun expandWithSourceJarSources(command: KotlinModel.CompilationTask): KotlinModel.CompilationTask =
        if (command.inputs.sourceJarsList.isEmpty()) {
            command
        } else {
            SourceJarExtractor(
                destDir = Paths.get(command.directories.temp).resolve("_srcjars"),
                fileMatcher = IS_JVM_SOURCE_FILE
            ).also {
                it.jarFiles.addAll(command.inputs.sourceJarsList.map { Paths.get(it) })
                it.execute()
            }.let {
                command.expandWithSources(it.sourcesList.iterator())
            }
        }

    override fun apply(args: List<String>): Int {
        return execute(args)
    }
}
