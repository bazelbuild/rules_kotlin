/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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

import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.model.JvmCompilationTask
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion as BtapiKotlinVersion
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import java.io.BufferedInputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Compiler that uses the Kotlin Build Tools API directly from protobuf task data.
 *
 * Unlike BuildToolsAPICompiler which parses CLI string arguments, this class
 * constructs BTAPI arguments directly from the JvmCompilationTask protobuf,
 * eliminating the string parsing layer.
 */
@OptIn(ExperimentalBuildToolsApi::class)
class BtapiCompiler(
  private val toolchains: KotlinToolchains,
  private val out: PrintStream,
) {
  /**
   * Compiles Kotlin sources using the Build Tools API.
   *
   * @param task The compilation task protobuf containing all compilation info
   * @param plugins Internal compiler plugins (jdeps, jvm-abi-gen, etc.)
   * @return CompilationResult indicating success or failure
   */
  fun compile(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
  ): CompilationResult {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    // Redirect stderr to capture compiler error messages
    val originalErr = System.err
    System.setErr(out)

    try {
      // Collect sources from protobuf
      val sources = (task.inputs.kotlinSourcesList + task.inputs.javaSourcesList)
        .map { Path.of(it) }
      val outputDir = Path.of(task.directories.classes)

      // Create BTAPI compilation operation
      val operation = toolchains.jvm.createJvmCompilationOperation(sources, outputDir)
      val compilerArgs = operation.compilerArguments

      // Configure compiler arguments directly from protobuf
      configureCompilerArguments(compilerArgs, task)

      // Configure plugin options (plugins are in the classloader, just need options)
      val pluginOptions = buildPluginOptions(task, plugins)
      if (pluginOptions.isNotEmpty()) {
//        compilerArgs.applyArgumentStrings(pluginOptions)
      }

      // Debug: print final arguments and classloader info
      System.err.println("DEBUG: final args = ${compilerArgs.toArgumentStrings()}")
      System.err.println("DEBUG: compilerArgs class = ${compilerArgs::class.java.name}")
      System.err.println("DEBUG: compilerArgs classloader = ${compilerArgs::class.java.classLoader}")
      System.err.println("DEBUG: toolchains class = ${toolchains::class.java.name}")
      System.err.println("DEBUG: toolchains classloader = ${toolchains::class.java.classLoader}")

      // Configure incremental compilation if enabled
      if (task.info.incrementalCompilation && task.directories.incrementalBaseDir.isNotEmpty()) {
//        configureIncrementalCompilation(operation, task, compilerArgs)
      }

      val logger = if (task.info.icEnableLogging) createIcLogger(out) else null

      // Execute the compilation
      val result = toolchains.createBuildSession().use { session ->
        session.executeOperation(operation, logger = logger)
      }

      return result
    } finally {
      System.setErr(originalErr)
    }
  }

  /**
   * Configures compiler arguments from protobuf fields using typed BTAPI setters.
   */
  @OptIn(ExperimentalCompilerArgument::class)
  private fun configureCompilerArguments(
    args: JvmCompilerArguments,
    task: JvmCompilationTask,
  ) {
    // Module name
    args[JvmCompilerArguments.MODULE_NAME] = task.info.moduleName

    // JVM target
    parseJvmTarget(task.info.toolchainInfo.jvm.jvmTarget)?.let {
      args[JvmCompilerArguments.JVM_TARGET] = it
    }

    // Language/API versions
    parseKotlinVersion(task.info.toolchainInfo.common.apiVersion)?.let {
      args[CommonCompilerArguments.API_VERSION] = it
    }
    parseKotlinVersion(task.info.toolchainInfo.common.languageVersion)?.let {
      args[CommonCompilerArguments.LANGUAGE_VERSION] = it
    }

    // Classpath - convert to absolute paths and pass via applyArgumentStrings
    val classpath = computeClasspath(task).map { File(it).absolutePath }
    System.err.println("DEBUG: classpath size = ${classpath.size}")
    classpath.forEach { System.err.println("DEBUG: classpath entry = $it") }
    if (classpath.isNotEmpty()) {
      args[JvmCompilerArguments.CLASSPATH] = classpath.joinToString(File.pathSeparator)
    }

    // Friend paths (for internal visibility)
    if (task.info.friendPathsList.isNotEmpty()) {
      args[JvmCompilerArguments.X_FRIEND_PATHS] = task.info.friendPathsList.toTypedArray()
    }

    // Passthrough flags (for args without typed setters)
    if (task.info.passthroughFlagsList.isNotEmpty()) {
      System.err.println("DEBUG: passthrough flags = ${task.info.passthroughFlagsList}")
//      args.applyArgumentStrings(task.info.passthroughFlagsList)
    }
  }

  /**
   * Computes the classpath for compilation.
   * Handles reduced classpath mode when enabled.
   */
  private fun computeClasspath(task: JvmCompilationTask): List<String> {
    val baseClasspath = when (task.info.reducedClasspathMode) {
      "KOTLINBUILDER_REDUCED" -> {
        val transitiveDepsForCompile = mutableSetOf<String>()
        task.inputs.depsArtifactsList.forEach { jdepsPath ->
          BufferedInputStream(Paths.get(jdepsPath).toFile().inputStream()).use {
            val deps = Deps.Dependencies.parseFrom(it)
            deps.dependencyList.forEach { dep ->
              if (dep.kind == Deps.Dependency.Kind.EXPLICIT) {
                transitiveDepsForCompile.add(dep.path)
              }
            }
          }
        }
        task.inputs.directDependenciesList + transitiveDepsForCompile
      }
      else -> task.inputs.classpathList
    }

    // Add generated classes directory to classpath
    return baseClasspath + task.directories.generatedClasses
  }

  /**
   * Builds plugin options for BTAPI. Plugins are loaded via classloader,
   * so we only need to pass their options, not -Xplugin.
   */
  private fun buildPluginOptions(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
  ): List<String> {
    val args = mutableListOf<String>()

    // JDeps plugin options
    if (task.outputs.jdeps.isNotEmpty()) {
      args.addAll(buildJdepsOptions(task, plugins.jdeps))
    }

    // JVM ABI Gen plugin options
    if (task.outputs.abijar.isNotEmpty()) {
      args.addAll(buildAbiGenOptions(task, plugins.jvmAbiGen))

      // Skip code gen if only generating ABI jar (no main output jar)
      if (task.outputs.jar.isEmpty()) {
        // skip-code-gen plugin has no options, just needs to be in classpath
      }
    }

    // User plugin options from protobuf
    task.inputs.compilerPluginOptionsList.forEach { opt ->
      args.add("-P")
      args.add("plugin:$opt")
    }

    return args
  }

  /**
   * Builds jdeps plugin options.
   */
  private fun buildJdepsOptions(
    task: JvmCompilationTask,
    jdeps: KotlinToolchain.CompilerPlugin,
  ): List<String> {
    val opts = mutableListOf<String>()
    opts.add("-P")
    opts.add("plugin:${jdeps.id}:output=${task.outputs.jdeps}")
    opts.add("-P")
    opts.add("plugin:${jdeps.id}:target_label=${task.info.label}")

    task.inputs.directDependenciesList.forEach {
      opts.add("-P")
      opts.add("plugin:${jdeps.id}:direct_dependencies=$it")
    }

    task.inputs.classpathList.forEach {
      opts.add("-P")
      opts.add("plugin:${jdeps.id}:full_classpath=$it")
    }

    opts.add("-P")
    opts.add("plugin:${jdeps.id}:strict_kotlin_deps=${task.info.strictKotlinDeps}")

    return opts
  }

  /**
   * Builds jvm-abi-gen plugin options.
   */
  private fun buildAbiGenOptions(
    task: JvmCompilationTask,
    abiGen: KotlinToolchain.CompilerPlugin,
  ): List<String> {
    val opts = mutableListOf<String>()
    opts.add("-P")
    opts.add("plugin:${abiGen.id}:outputDir=${task.directories.abiClasses}")

    if (task.info.treatInternalAsPrivateInAbiJar) {
      opts.add("-P")
      opts.add("plugin:${abiGen.id}:treatInternalAsPrivate=true")
    }
    if (task.info.removePrivateClassesInAbiJar) {
      opts.add("-P")
      opts.add("plugin:${abiGen.id}:removePrivateClasses=true")
    }
    if (task.info.removeDebugInfo) {
      opts.add("-P")
      opts.add("plugin:${abiGen.id}:removeDebugInfo=true")
    }

    return opts
  }

  /**
   * Configures incremental compilation for the operation.
   */
  private fun configureIncrementalCompilation(
    operation: org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation,
    task: JvmCompilationTask,
    compilerArgs: JvmCompilerArguments,
  ) {
    val icBaseDir = Path.of(task.directories.incrementalBaseDir)
    val icWorkingDir = icBaseDir.resolve("ic-caches")
    val shrunkSnapshot = icBaseDir.resolve("shrunk-classpath-snapshot.bin")

    // Compute force recompilation based on args hash
    val currentArgsHash = computeArgsHash(compilerArgs, task)
    val previousArgsHash = loadArgsHash(icBaseDir)
    val snapshotMissing = !shrunkSnapshot.toFile().exists()
    val argsChanged = previousArgsHash != null && previousArgsHash != currentArgsHash
    val forceRecompilation = snapshotMissing || argsChanged

    // Store current hash for next build
    Files.createDirectories(icBaseDir)
    storeArgsHash(icBaseDir, currentArgsHash)

    // Compute classpath snapshot paths
    val classpathSnapshots = task.createClasspathSnapshotsPaths().map { Path.of(it) }

    val icOptions = operation.createSnapshotBasedIcOptions().apply {
      this[ROOT_PROJECT_DIR] = Path.of(ROOT)
      this[MODULE_BUILD_DIR] = Path.of(task.directories.classes).parent ?: Path.of(task.directories.classes)
      this[FORCE_RECOMPILATION] = forceRecompilation
      this[OUTPUT_DIRS] = setOf(Path.of(task.directories.classes), icWorkingDir)
    }

    operation[INCREMENTAL_COMPILATION] = JvmSnapshotBasedIncrementalCompilationConfiguration(
      workingDirectory = icWorkingDir,
      sourcesChanges = SourcesChanges.ToBeCalculated,
      dependenciesSnapshotFiles = classpathSnapshots,
      shrunkClasspathSnapshot = shrunkSnapshot,
      options = icOptions,
    )
  }

  /**
   * Computes a hash of compiler configuration for detecting changes.
   */
  private fun computeArgsHash(args: JvmCompilerArguments, task: JvmCompilationTask): Long {
    // Hash relevant settings that would require recompilation if changed
    var hash = 0L
    hash = hash * 31 + task.info.moduleName.hashCode()
    hash = hash * 31 + task.info.toolchainInfo.jvm.jvmTarget.hashCode()
    hash = hash * 31 + task.info.toolchainInfo.common.apiVersion.hashCode()
    hash = hash * 31 + task.info.toolchainInfo.common.languageVersion.hashCode()
    hash = hash * 31 + task.info.passthroughFlagsList.sorted().hashCode()
    hash = hash * 31 + task.inputs.compilerPluginOptionsList.sorted().hashCode()
    return hash
  }

  private fun storeArgsHash(icBaseDir: Path, hash: Long) {
    val hashFile = icBaseDir.resolve("args-hash.txt")
    Files.writeString(hashFile, hash.toString())
  }

  private fun loadArgsHash(icBaseDir: Path): Long? {
    val hashFile = icBaseDir.resolve("args-hash.txt")
    return if (Files.exists(hashFile)) {
      Files.readString(hashFile).trim().toLongOrNull()
    } else {
      null
    }
  }

  /**
   * Parse JVM target string to BTAPI enum.
   */
  private fun parseJvmTarget(target: String): JvmTarget? =
    when (target) {
      "1.6", "6" -> JvmTarget.JVM1_6
      "1.8", "8" -> JvmTarget.JVM1_8
      "9" -> JvmTarget.JVM_9
      "10" -> JvmTarget.JVM_10
      "11" -> JvmTarget.JVM_11
      "12" -> JvmTarget.JVM_12
      "13" -> JvmTarget.JVM_13
      "14" -> JvmTarget.JVM_14
      "15" -> JvmTarget.JVM_15
      "16" -> JvmTarget.JVM_16
      "17" -> JvmTarget.JVM_17
      "18" -> JvmTarget.JVM_18
      "19" -> JvmTarget.JVM_19
      "20" -> JvmTarget.JVM_20
      "21" -> JvmTarget.JVM_21
      "22" -> JvmTarget.JVM_22
      "23" -> JvmTarget.JVM_23
      "24" -> JvmTarget.JVM_24
      "25" -> JvmTarget.JVM_25
      else -> null
    }

  /**
   * Parse Kotlin version string to BTAPI enum.
   */
  private fun parseKotlinVersion(version: String): BtapiKotlinVersion? =
    when (version) {
      "1.4" -> BtapiKotlinVersion.V1_4
      "1.5" -> BtapiKotlinVersion.V1_5
      "1.6" -> BtapiKotlinVersion.V1_6
      "1.7" -> BtapiKotlinVersion.V1_7
      "1.8" -> BtapiKotlinVersion.V1_8
      "1.9" -> BtapiKotlinVersion.V1_9
      "2.0" -> BtapiKotlinVersion.V2_0
      "2.1" -> BtapiKotlinVersion.V2_1
      "2.2" -> BtapiKotlinVersion.V2_2
      "2.3" -> BtapiKotlinVersion.V2_3
      else -> null
    }

  /**
   * Creates a simple logger for IC debugging output.
   */
  private fun createIcLogger(out: PrintStream): KotlinLogger = object : KotlinLogger {
    override val isDebugEnabled: Boolean = true

    override fun error(msg: String, throwable: Throwable?) {
      out.println("[IC ERROR] $msg")
      throwable?.printStackTrace(out)
    }

    override fun warn(msg: String, throwable: Throwable?) {
      out.println("[IC WARN] $msg")
    }

    override fun info(msg: String) {
      out.println("[IC INFO] $msg")
    }

    override fun debug(msg: String) {
      out.println("[IC DEBUG] $msg")
    }

    override fun lifecycle(msg: String) {
      out.println("[IC] $msg")
    }
  }
}
