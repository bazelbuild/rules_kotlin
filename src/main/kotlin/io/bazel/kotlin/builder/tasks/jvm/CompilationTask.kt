/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Provides extensions for the JvmCompilationTask protocol buffer.
package io.bazel.kotlin.builder.tasks.jvm

import io.bazel.kotlin.builder.tasks.jvm.JDepsGenerator.emptyJdeps
import io.bazel.kotlin.builder.tasks.jvm.JDepsGenerator.writeJdeps
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.utils.IS_JVM_SOURCE_FILE
import io.bazel.kotlin.builder.utils.bazelRuleKind
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.JarHelper.Companion.MANIFEST_DIR
import io.bazel.kotlin.builder.utils.jars.SourceJarExtractor
import io.bazel.kotlin.builder.utils.partitionJvmSources
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.kotlin.model.JvmCompilationTask.Directories
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Files.isDirectory
import java.nio.file.Files.walk
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.Stream
import kotlin.io.path.exists

private const val SOURCE_JARS_DIR = "_srcjars"

internal fun JvmCompilationTask.plugins(
  options: List<String>,
  classpath: List<String>,
): CompilationArgs =
  CompilationArgs().apply {
    classpath.forEach {
      xFlag("plugin", it)
    }

    val optionTokens =
      mapOf(
        "{generatedClasses}" to directories.generatedClasses,
        "{stubs}" to directories.stubs,
        "{temp}" to directories.temp,
        "{generatedSources}" to directories.generatedSources,
        "{classpath}" to classpath.joinToString(File.pathSeparator),
      )
    options.forEach { opt ->
      val formatted =
        optionTokens.entries.fold(opt) { formatting, (token, value) ->
          formatting.replace(token, value)
        }
      flag("-P", "plugin:$formatted")
    }
  }

internal fun JvmCompilationTask.preProcessingSteps(
  context: CompilationTaskContext,
): JvmCompilationTask = context.execute("expand sources") { expandWithSourceJarSources() }

@org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
internal fun JvmCompilationTask.runPlugins(
  context: CompilationTaskContext,
  plugins: InternalCompilerPlugins,
  btapiCompiler: BtapiCompiler,
): JvmCompilationTask {
  if (
    (
      inputs.processorsList.isEmpty() &&
        inputs.stubsPluginClasspathList.isEmpty()
    ) ||
    inputs.kotlinSourcesList.isEmpty()
  ) {
    return this
  } else {
    return runKaptPluginBtapi(context, plugins, btapiCompiler)
  }
}

/**
 * Runs KAPT using BtapiCompiler (Build Tools API).
 *
 * This is the new implementation that uses BTAPI directly instead of
 * the string-based KotlincInvoker path.
 */
@org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
private fun JvmCompilationTask.runKaptPluginBtapi(
  context: CompilationTaskContext,
  plugins: InternalCompilerPlugins,
  btapiCompiler: BtapiCompiler,
): JvmCompilationTask =
  context.execute("kapt (${inputs.processorsList.joinToString(", ")})") {
    val result =
      btapiCompiler.compileKapt(
        task = this,
        plugins = plugins,
        aptMode = "stubsAndApt",
        verbose = context.whenTracing { true } == true,
        out = context.out,
      )

    when (result) {
      CompilationResult.COMPILATION_SUCCESS -> {
        context.whenTracing {
          printLines("kapt btapi", listOf("KAPT completed successfully"))
        }
        expandWithGeneratedSources()
      }
      CompilationResult.COMPILATION_ERROR ->
        throw CompilationStatusException("KAPT failed", 1)
      CompilationResult.COMPILATION_OOM_ERROR ->
        throw CompilationStatusException("KAPT failed with OOM", 3)
      CompilationResult.COMPILER_INTERNAL_ERROR ->
        throw CompilationStatusException("KAPT compiler internal error", 2)
    }
  }

/**
 * Produce the primary output jar.
 */
internal fun JvmCompilationTask.createOutputJar() =
  JarCreator(
    path = Paths.get(outputs.jar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.classes))
    it.addDirectory(Paths.get(directories.javaClasses))
    it.addDirectory(Paths.get(directories.generatedClasses))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }

/**
 * Produce the primary output jar.
 */
internal fun JvmCompilationTask.createAbiJar() =
  JarCreator(
    path = Paths.get(outputs.abijar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.abiClasses))
    it.addDirectory(Paths.get(directories.generatedClasses))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }

/**
 * Produce a jar of sources generated by KAPT.
 */
internal fun JvmCompilationTask.createGeneratedJavaSrcJar() {
  JarCreator(
    path = Paths.get(outputs.generatedJavaSrcJar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.generatedJavaSources))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }
}

/**
 * Produce a stub jar of classes generated by KAPT.
 */
internal fun JvmCompilationTask.createGeneratedStubJar() {
  JarCreator(
    path = Paths.get(outputs.generatedJavaStubJar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.incrementalData))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }
}

/**
 * Produce a jar of classes generated by KAPT.
 */
internal fun JvmCompilationTask.createGeneratedClassJar() {
  JarCreator(
    path = Paths.get(outputs.generatedClassJar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.generatedClasses))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }
}

internal fun JvmCompilationTask.createGeneratedKspKotlinSrcJar() {
  JarCreator(
    path = Paths.get(outputs.generatedKspSrcJar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.generatedSources))
    it.addDirectory(Paths.get(directories.generatedJavaSources))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }
}

/**
 * Produce a jar of classes generated by KSP.
 */
internal fun JvmCompilationTask.createdGeneratedKspClassesJar() {
  JarCreator(
    path = Paths.get(outputs.generatedKspClassesJar),
    normalize = true,
    verbose = false,
  ).also {
    it.addDirectory(Paths.get(directories.generatedClasses))
    it.setJarOwner(info.label, info.bazelRuleKind)
    it.execute()
  }
}

/**
 * Creates a classpath snapshot for the output jar.
 * The snapshot is stored in the IC directory as a worker-local side-effect,
 * not as a Bazel-tracked output.
 *
 * This snapshot (output-classpath-snapshot.bin) is used by downstream targets
 * that depend on this target. It is separate from BTAPI's internal shrunk
 * dependencies snapshot (shrunk-classpath-snapshot.bin).
 */
@OptIn(ExperimentalBuildToolsApi::class)
internal fun JvmCompilationTask.createOutputClasspathSnapshot(btapiCompiler: BtapiCompiler) {
  if (!info.incrementalCompilation || directories.incrementalBaseDir.isEmpty()) {
    return
  }
  // Write snapshot to IC directory - use distinct name to avoid collision with BTAPI's internal snapshot
  val icDir = Paths.get(directories.incrementalBaseDir)
  Files.createDirectories(icDir)
  val snapshotPath = icDir.resolve("output-classpath-snapshot.bin")

  btapiCompiler.generateClasspathSnapshot(
    Paths.get(outputs.jar),
    snapshotPath,
    ClassSnapshotGranularity.CLASS_MEMBER_LEVEL,
  )
}

/**
 * Caches the jdeps file to IC directory for future incremental builds.
 * This is called after successful compilation so the jdeps can be restored
 * when IC skips compilation in future builds.
 */
internal fun JvmCompilationTask.cacheJdepsToIcDir() {
  if (!info.incrementalCompilation || directories.incrementalBaseDir.isEmpty()) {
    return
  }
  if (outputs.jdeps.isEmpty()) {
    return
  }
  val jdepsPath = Paths.get(outputs.jdeps)
  if (!jdepsPath.exists()) {
    return
  }
  val icDir = Paths.get(directories.incrementalBaseDir)
  Files.createDirectories(icDir)
  val cachedJdepsPath = icDir.resolve("cached.jdeps")
  Files.copy(jdepsPath, cachedJdepsPath, StandardCopyOption.REPLACE_EXISTING)
}

/**
 * Ensures the jdeps file exists. If IC skipped compilation and jdeps wasn't created,
 * this restores the cached jdeps from IC directory or writes an empty jdeps.
 */
internal fun JvmCompilationTask.ensureJdepsExists() {
  if (outputs.jdeps.isEmpty()) {
    return
  }
  val jdepsPath = Paths.get(outputs.jdeps)
  if (jdepsPath.exists()) {
    // jdeps was created by the compiler, cache it for future IC builds
    cacheJdepsToIcDir()
    return
  }
  // jdeps wasn't created (IC skipped compilation), try to restore from cache
  if (info.incrementalCompilation && directories.incrementalBaseDir.isNotEmpty()) {
    val icDir = Paths.get(directories.incrementalBaseDir)
    val cachedJdepsPath = icDir.resolve("cached.jdeps")
    if (cachedJdepsPath.exists()) {
      Files.copy(cachedJdepsPath, jdepsPath, StandardCopyOption.REPLACE_EXISTING)
      return
    }
  }
  // No cached jdeps, write an empty one
  writeJdeps(outputs.jdeps, emptyJdeps(info.label))
}

/**
 * Computes classpath snapshot paths from classpath JAR paths.
 * For each JAR at path/foo.jar or path/foo.abi.jar, the snapshot is at path/foo-ic/output-classpath-snapshot.bin.
 * The IC directory is always derived from the main JAR name (without .abi suffix).
 * Only returns paths that actually exist (from previous builds).
 *
 * Note: This looks for output-classpath-snapshot.bin (the snapshot of the dep's output JAR),
 * not shrunk-classpath-snapshot.bin (which is BTAPI's internal file).
 */
internal fun JvmCompilationTask.createClasspathSnapshotsPaths(): List<String> {
  if (!info.incrementalCompilation) {
    return emptyList()
  }
  return inputs.classpathList
    .mapNotNull { jarPath ->
      val path = Paths.get(jarPath)
      // Handle both main jars (foo.jar) and ABI jars (foo.abi.jar)
      // IC directory is always derived from the main jar name
      val jarName =
        path.fileName
          .toString()
          .removeSuffix(".jar")
          .removeSuffix(".abi") // For ABI jars, remove .abi suffix too
      val snapshotPath = path.resolveSibling("$jarName-ic/output-classpath-snapshot.bin")
      if (snapshotPath.exists()) {
        snapshotPath.toString()
      } else {
        null
      }
    }
}

val ROOT: String by lazy {
  FileSystems
    .getDefault()
    .getPath("")
    .toAbsolutePath()
    .toString() + File.separator
}

/**
 * If any srcjars were provided expand the jars sources and create a new [JvmCompilationTask] with the
 * Java, Kotlin sources and META folder merged in.
 */
internal fun JvmCompilationTask.expandWithSourceJarSources(): JvmCompilationTask =
  if (inputs.sourceJarsList.isEmpty()) {
    this
  } else {
    expandWithSources(
      SourceJarExtractor(
        destDir = Paths.get(directories.temp).resolve(SOURCE_JARS_DIR),
        fileMatcher = { str: String -> IS_JVM_SOURCE_FILE.test(str) || "/$MANIFEST_DIR" in str },
      ).also {
        it.jarFiles.addAll(inputs.sourceJarsList.map { p -> Paths.get(p) })
        it.execute()
      }.sourcesList
        .iterator(),
    )
  }

private val Directories.stubs
  get() =
    Files
      .createDirectories(
        Paths
          .get(temp)
          .resolve("stubs"),
      ).toString()
private val Directories.incrementalData
  get() =
    Files
      .createDirectories(
        Paths
          .get(temp)
          .resolve("incrementalData"),
      ).toString()

/**
 * Create a new [JvmCompilationTask] with sources found in the generatedSources directory. This should be run after
 * annotation processors have been run.
 */
fun JvmCompilationTask.expandWithGeneratedSources(): JvmCompilationTask =
  expandWithSources(
    Stream
      .of(directories.generatedSources, directories.generatedJavaSources)
      .map { s -> Paths.get(s) }
      .flatMap { p -> walk(p) }
      .filter { !isDirectory(it) }
      .map { it.toString() }
      .distinct()
      .iterator(),
  )

private fun JvmCompilationTask.expandWithSources(sources: Iterator<String>): JvmCompilationTask =
  updateBuilder { builder ->
    val inputs = builder.inputsBuilder
    sources
      .copyManifestFilesToGeneratedClasses(directories)
      .filterOutNonCompilableSources()
      .partitionJvmSources({ inputs.addKotlinSources(it) }, { inputs.addJavaSources(it) })
  }

private fun JvmCompilationTask.updateBuilder(
  block: (JvmCompilationTask.Builder) -> Unit,
): JvmCompilationTask =
  toBuilder().let {
    block(it)
    it.build()
  }

/**
 * Copy generated manifest files from KSP task into generated folder
 */
internal fun Iterator<String>.copyManifestFilesToGeneratedClasses(
  directories: Directories,
): Iterator<String> {
  val result = mutableSetOf<String>()
  this.forEach {
    if ("/$MANIFEST_DIR" in it) {
      val path = Paths.get(it)
      val srcJarsPath = Paths.get(directories.temp, SOURCE_JARS_DIR)
      if (srcJarsPath.exists()) {
        val relativePath = srcJarsPath.relativize(path)
        val destPath = Paths.get(directories.generatedClasses).resolve(relativePath)
        destPath.parent.toFile().mkdirs()
        Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING)
      }
    }
    result.add(it)
  }
  return result.iterator()
}

/**
 * Only keep java and kotlin files for the iterator. Filter our all other non-compilable files.
 */
private fun Iterator<String>.filterOutNonCompilableSources(): Iterator<String> {
  val result = mutableListOf<String>()
  this.forEach {
    if (it.endsWith(".kt") or it.endsWith(".java")) result.add(it)
  }
  return result.iterator()
}
