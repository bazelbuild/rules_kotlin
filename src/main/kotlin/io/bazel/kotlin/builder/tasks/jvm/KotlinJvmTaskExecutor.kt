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
import io.bazel.kotlin.builder.utils.*
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarExtractor
import io.bazel.kotlin.model.JvmCompilationTask
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Due to an inconsistency in the handling of -Xfriends-path, jvm uses a comma (property list
 * separator), js uses the system path separator.
 */
const val X_FRIENDS_PATH_SEPARATOR = ","

@Singleton
class KotlinJvmTaskExecutor @Inject internal constructor(
    private val compiler: KotlinToolchain.KotlincInvoker,
    private val pluginArgsEncoder: KotlinCompilerPluginArgsEncoder,
    private val javaCompiler: JavaCompiler,
    private val jDepsGenerator: JDepsGenerator
) {
    fun execute(context: CompilationTaskContext, task: JvmCompilationTask) {
        val preprocessedTask = task.preProcessingSteps(context)
        context.execute("compile classes") { preprocessedTask.compileAll(context) }
        context.execute("create jar") { preprocessedTask.createOutputJar() }
        context.execute("produce src jar") { preprocessedTask.produceSourceJar() }
        context.execute("generate jdeps") { jDepsGenerator.generateJDeps(preprocessedTask) }
    }

    private fun JvmCompilationTask.preProcessingSteps(context: CompilationTaskContext): JvmCompilationTask {
        ensureDirectories(
            directories.temp,
            directories.generatedSources,
            directories.generatedClasses
        )
        val taskWithAdditionalSources = context.execute("expand sources") { expandWithSourceJarSources() }
        return context.execute({
            "kapt (${inputs.processorsList.joinToString(", ")})"
        }) { taskWithAdditionalSources.runAnnotationProcessors(context) }
    }

    private fun JvmCompilationTask.produceSourceJar() {
        Paths.get(outputs.srcjar).also { sourceJarPath ->
            Files.createFile(sourceJarPath)
            SourceJarCreator(
                sourceJarPath
            ).also { creator ->
                // This check asserts that source jars were unpacked if present.
                check(
                    inputs.sourceJarsList.isEmpty() ||
                            Files.exists(Paths.get(directories.temp).resolve("_srcjars"))
                )
                listOf(
                    // Any (input) source jars should already have been expanded so do not add them here.
                    inputs.javaSourcesList.stream(),
                    inputs.kotlinSourcesList.stream()
                ).stream()
                    .flatMap { it.map { p -> Paths.get(p) } }
                    .also { creator.addSources(it) }
                creator.execute()
            }
        }
    }

    private fun JvmCompilationTask.runAnnotationProcessor(
        context: CompilationTaskContext,
        printOnSuccess: Boolean = true
    ): List<String> {
        check(inputs.processorsList.isNotEmpty()) { "method called without annotation processors" }
        return getCommonArgs().let { args ->
            args.addAll(pluginArgsEncoder.encode(context, this))
            args.addAll(inputs.kotlinSourcesList)
            args.addAll(inputs.javaSourcesList)
            context.executeCompilerTask(args, compiler::compile, printOnSuccess = printOnSuccess)
        }
    }

    /**
     * Return a list with the common arguments.
     */
    private fun JvmCompilationTask.getCommonArgs(): MutableList<String> {
        val args = mutableListOf<String>()
        val friendPaths = info.friendPathsList.map { Paths.get(it).toAbsolutePath() }
        val cp = inputs.joinedClasspath
            .split(File.pathSeparator)
            .map { Paths.get(it).toAbsolutePath() }
            .joinToString(File.pathSeparator)
        args.addAll(
            "-cp", cp,
            "-api-version", info.toolchainInfo.common.apiVersion,
            "-language-version", info.toolchainInfo.common.languageVersion,
            "-jvm-target", info.toolchainInfo.jvm.jvmTarget,
            "-Xfriend-paths=${friendPaths.joinToString(X_FRIENDS_PATH_SEPARATOR)}"
        )
        args
            .addAll("-module-name", info.moduleName)
            .addAll("-d", directories.classes)

        info.passthroughFlags?.takeIf { it.isNotBlank() }?.also { args.addAll(it.split(" ")) }
        return args
    }

    private fun JvmCompilationTask.runAnnotationProcessors(
        context: CompilationTaskContext
    ): JvmCompilationTask =
        if (inputs.processorsList.isEmpty()) {
            this
        } else {
            runAnnotationProcessor(context, printOnSuccess = !context.isTracing).let { outputLines ->
                // if tracing is enabled the output should be formatted in a special way, if we aren't tracing then any
                // compiler output would make it's way to the console as is.
                if (context.isTracing) {
                    context.printLines("kapt output", outputLines)
                }
                expandWithGeneratedSources()
            }
        }

    /**
     * Produce the primary output jar.
     */
    private fun JvmCompilationTask.createOutputJar() =
        JarCreator(
            path = Paths.get(outputs.jar),
            normalize = true,
            verbose = false
        ).also {
            it.addDirectory(Paths.get(directories.classes))
            it.addDirectory(Paths.get(directories.generatedClasses))
            it.setJarOwner(info.label, info.bazelRuleKind)
            it.execute()
        }

    private fun JvmCompilationTask.compileAll(context: CompilationTaskContext) {
        ensureDirectories(
            directories.classes
        )
        var kotlinError: CompilationStatusException? = null
        var result: List<String>? = null
        context.execute("kotlinc") {
            result = try {
                compileKotlin(context, printOnFail = false)
            } catch (ex: CompilationStatusException) {
                kotlinError = ex
                ex.lines
            }
        }
        try {
            context.execute("javac") { javaCompiler.compile(context, this) }
        } finally {
            checkNotNull(result).also(context::printCompilerOutput)
            kotlinError?.also { throw it }
        }
    }

    /**
     * Compiles Kotlin sources to classes. Does not compile Java sources.
     */
    private fun JvmCompilationTask.compileKotlin(context: CompilationTaskContext, printOnFail: Boolean = true) =
        getCommonArgs().let { args ->
            args.addAll(inputs.javaSourcesList)
            args.addAll(inputs.kotlinSourcesList)
            if ( inputs.classpathList.any { classpath -> "kotlin-android-extensions-runtime" in classpath } ) {
                inputs.classpathList.firstOrNull { classpath -> "android-extensions-compiler" in classpath }
                    ?.let { compiler ->
                        args += "-Xplugin=$compiler"
                        args += "-P"
                        args += "plugin:org.jetbrains.kotlin.android:experimental=true"
                    }
            }
            context.executeCompilerTask(args, compiler::compile, printOnFail = printOnFail)
        }

    /**
     * If any srcjars were provided expand the jars sources and create a new [JvmCompilationTask] with the
     * Java and Kotlin sources merged in.
     */
    private fun JvmCompilationTask.expandWithSourceJarSources(): JvmCompilationTask =
        if (inputs.sourceJarsList.isEmpty())
            this
        else expandWithSources(
            SourceJarExtractor(
                destDir = Paths.get(directories.temp).resolve("_srcjars"),
                fileMatcher = IS_JVM_SOURCE_FILE
            ).also {
                it.jarFiles.addAll(inputs.sourceJarsList.map { p -> Paths.get(p) })
                it.execute()
            }.sourcesList.iterator()
        )

    /**
     * Create a new [JvmCompilationTask] with sources found in the generatedSources directory. This should be run after
     * annotation processors have been run.
     */
    private fun JvmCompilationTask.expandWithGeneratedSources(): JvmCompilationTask =
        expandWithSources(
            File(directories.generatedSources).walkTopDown()
                .filter { it.isFile }
                .map { it.path }
                .iterator()
        )

    private fun JvmCompilationTask.expandWithSources(sources: Iterator<String>): JvmCompilationTask =
        updateBuilder { builder ->
            sources.partitionJvmSources(
                { builder.inputsBuilder.addKotlinSources(it) },
                { builder.inputsBuilder.addJavaSources(it) })
        }

    private fun JvmCompilationTask.updateBuilder(
        block: (JvmCompilationTask.Builder) -> Unit
    ): JvmCompilationTask =
        toBuilder().let {
            block(it)
            it.build()
        }
}
