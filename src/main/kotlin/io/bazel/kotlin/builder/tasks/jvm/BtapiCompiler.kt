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
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import java.util.Base64
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion as BtapiKotlinVersion

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
) {
  private val buildSession by lazy { toolchains.createBuildSession() }

  /**
   * Generates a classpath snapshot for the given input JAR.
   *
   * The snapshot is used by incremental compilation to detect changes in dependencies.
   * This method uses hash-based caching to avoid regenerating snapshots when the
   * input JAR hasn't changed.
   *
   * @param inputJar Path to the input JAR file
   * @param outputSnapshot Path where the snapshot should be written
   * @param granularity Snapshot granularity (CLASS_LEVEL or CLASS_MEMBER_LEVEL)
   */
  fun generateClasspathSnapshot(
    inputJar: Path,
    outputSnapshot: Path,
    granularity: ClassSnapshotGranularity,
  ) {
    val hashPath = outputSnapshot.resolveSibling(outputSnapshot.fileName.toString() + ".hash")

    // Check if snapshot is up-to-date (caching)
    if (outputSnapshot.exists() && hashPath.exists()) {
      val storedHash = Files.readAllLines(hashPath).firstOrNull()?.trim()
      val currentHash = hashFile(inputJar)
      if (storedHash == currentHash) {
        return // Snapshot is up-to-date
      }
    }

    // Generate snapshot
    val snapshotOperation = toolchains.jvm.createClasspathSnapshottingOperation(inputJar)
    snapshotOperation.set(JvmClasspathSnapshottingOperation.GRANULARITY, granularity)
    snapshotOperation.set(PARSE_INLINED_LOCAL_CLASSES, true)

    val snapshot = buildSession.executeOperation(snapshotOperation)

    // Write to temp files first, then atomically move
    val currentHash = hashFile(inputJar)
    val tempSnapshot = outputSnapshot.resolveSibling(outputSnapshot.fileName.toString() + ".tmp")
    val tempHash = hashPath.resolveSibling(hashPath.fileName.toString() + ".tmp")

    snapshot.saveSnapshot(tempSnapshot)
    tempHash.toFile().writeText(currentHash)

    Files.move(tempSnapshot, outputSnapshot, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    Files.move(tempHash, hashPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
  }

  private fun hashFile(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(path.toFile()).use { fis ->
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (fis.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

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
    out: PrintStream,
  ): CompilationResult {
    val compilerPlugins = buildCompilerPlugins(task, plugins)
    val logger = if (task.info.icEnableLogging) createIcLogger(out) else null

    return executeCompilation(
      task = task,
      outputDir = Path.of(task.directories.classes),
      compilerPlugins = compilerPlugins,
      logger = logger,
      out = out,
    ) { operation, compilerArgs ->
      // Configure incremental compilation if enabled
      if (task.info.incrementalCompilation && task.directories.incrementalBaseDir.isNotEmpty()) {
        configureIncrementalCompilation(operation, task, compilerArgs)
      }
    }
  }

  /**
   * Common compilation execution logic shared by compile and compileKapt.
   */
  private fun executeCompilation(
    task: JvmCompilationTask,
    outputDir: Path,
    compilerPlugins: List<CompilerPlugin>,
    logger: KotlinLogger?,
    out: PrintStream,
    additionalConfiguration: (
      org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation,
      JvmCompilerArguments,
    ) -> Unit = { _, _ -> },
  ): CompilationResult {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    // Redirect stderr to capture compiler error messages
    val originalErr = System.err
    System.setErr(out)

    try {
      // Collect sources from protobuf
      val sources =
        (task.inputs.kotlinSourcesList + task.inputs.javaSourcesList)
          .map { Path.of(it) }

      // Create BTAPI compilation operation
      val operation = toolchains.jvm.createJvmCompilationOperation(sources, outputDir)
      val compilerArgs = operation.compilerArguments

      // Configure compiler arguments directly from protobuf
      configureCompilerArguments(compilerArgs, task)

      // Configure compiler plugins
      if (compilerPlugins.isNotEmpty()) {
        compilerArgs[CommonCompilerArguments.COMPILER_PLUGINS] = compilerPlugins
      }

      // Allow caller to do additional configuration
      additionalConfiguration(operation, compilerArgs)

      // Execute the compilation
      return buildSession.executeOperation(operation, logger = logger)
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
    args[JvmCompilerArguments.NO_STDLIB] = true
    args[JvmCompilerArguments.NO_REFLECT] = true

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
    val baseClasspath =
      when (task.info.reducedClasspathMode) {
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
   * Builds compiler plugins for BTAPI using the typed CompilerPlugin API.
   */
  private fun buildCompilerPlugins(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
  ): List<CompilerPlugin> {
    val result = mutableListOf<CompilerPlugin>()

    // JDeps plugin
    if (task.outputs.jdeps.isNotEmpty()) {
      result.add(buildJdepsPlugin(task, plugins.jdeps))
    }

    // JVM ABI Gen plugin
    if (task.outputs.abijar.isNotEmpty()) {
      result.add(buildAbiGenPlugin(task, plugins.jvmAbiGen))

      // Skip code gen if only generating ABI jar (no main output jar)
      if (task.outputs.jar.isEmpty()) {
        result.add(buildSkipCodeGenPlugin(plugins.skipCodeGen))
      }
    }

    // User plugins from protobuf
    result.addAll(buildUserPlugins(task))

    return result
  }

  /**
   * Builds the skip-code-gen plugin (has no options, just classpath).
   */
  private fun buildSkipCodeGenPlugin(skipCodeGen: KotlinToolchain.CompilerPlugin): CompilerPlugin =
    CompilerPlugin(
      pluginId = skipCodeGen.id,
      classpath = listOf(Path.of(skipCodeGen.jarPath)),
      rawArguments = emptyList(),
      orderingRequirements = emptySet(),
    )

  /**
   * Builds user-specified compiler plugins from protobuf options.
   */
  private fun buildUserPlugins(task: JvmCompilationTask): List<CompilerPlugin> {
    // Group plugin options by plugin ID
    val pluginOptionsMap = mutableMapOf<String, MutableList<CompilerPluginOption>>()

    task.inputs.compilerPluginOptionsList.forEach { optionStr ->
      // Format is "pluginId:key=value"
      val colonIdx = optionStr.indexOf(':')
      if (colonIdx > 0) {
        val pluginId = optionStr.substring(0, colonIdx)
        val keyValue = optionStr.substring(colonIdx + 1)
        val eqIdx = keyValue.indexOf('=')
        if (eqIdx > 0) {
          val key = keyValue.substring(0, eqIdx)
          val value = keyValue.substring(eqIdx + 1)
          pluginOptionsMap
            .getOrPut(pluginId) { mutableListOf() }
            .add(CompilerPluginOption(key, value))
        }
      }
    }

    // All user plugins share the same classpath
    val userPluginClasspath = task.inputs.compilerPluginClasspathList.map { Path.of(it) }

    return pluginOptionsMap.map { (pluginId, options) ->
      CompilerPlugin(
        pluginId = pluginId,
        classpath = userPluginClasspath,
        rawArguments = options,
        orderingRequirements = emptySet(),
      )
    }
  }

  /**
   * Builds jdeps plugin using the typed CompilerPlugin API.
   */
  private fun buildJdepsPlugin(
    task: JvmCompilationTask,
    jdeps: KotlinToolchain.CompilerPlugin,
  ): CompilerPlugin {
    val options = mutableListOf<CompilerPluginOption>()

    options.add(CompilerPluginOption("output", task.outputs.jdeps))
    options.add(CompilerPluginOption("target_label", task.info.label))

    task.inputs.directDependenciesList.forEach {
      options.add(CompilerPluginOption("direct_dependencies", it))
    }

    task.inputs.classpathList.forEach {
      options.add(CompilerPluginOption("full_classpath", it))
    }

    options.add(CompilerPluginOption("strict_kotlin_deps", task.info.strictKotlinDeps))

    return CompilerPlugin(
      pluginId = jdeps.id,
      classpath = listOf(Path.of(jdeps.jarPath)),
      rawArguments = options,
      orderingRequirements = emptySet(),
    )
  }

  /**
   * Builds jvm-abi-gen plugin using the typed CompilerPlugin API.
   */
  private fun buildAbiGenPlugin(
    task: JvmCompilationTask,
    abiGen: KotlinToolchain.CompilerPlugin,
  ): CompilerPlugin {
    val options = mutableListOf<CompilerPluginOption>()

    options.add(CompilerPluginOption("outputDir", task.directories.abiClasses))

    if (task.info.treatInternalAsPrivateInAbiJar) {
      options.add(CompilerPluginOption("treatInternalAsPrivate", "true"))
    }
    if (task.info.removePrivateClassesInAbiJar) {
      options.add(CompilerPluginOption("removePrivateClasses", "true"))
    }
    if (task.info.removeDebugInfo) {
      options.add(CompilerPluginOption("removeDebugInfo", "true"))
    }

    return CompilerPlugin(
      pluginId = abiGen.id,
      classpath = listOf(Path.of(abiGen.jarPath)),
      rawArguments = options,
      orderingRequirements = emptySet(),
    )
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

    val icOptions =
      operation.createSnapshotBasedIcOptions().apply {
        this[ROOT_PROJECT_DIR] = Path.of(ROOT)
        this[MODULE_BUILD_DIR] =
          Path.of(task.directories.classes).parent ?: Path.of(task.directories.classes)
        this[FORCE_RECOMPILATION] = forceRecompilation
        this[OUTPUT_DIRS] = setOf(Path.of(task.directories.classes), icWorkingDir)
      }

    operation[INCREMENTAL_COMPILATION] =
      JvmSnapshotBasedIncrementalCompilationConfiguration(
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
  private fun computeArgsHash(
    args: JvmCompilerArguments,
    task: JvmCompilationTask,
  ): Long {
    // Hash relevant settings that would require recompilation if changed
    var hash = 0L
    hash = hash * 31 + task.info.moduleName.hashCode()
    hash = hash * 31 +
      task.info.toolchainInfo.jvm.jvmTarget
        .hashCode()
    hash = hash * 31 +
      task.info.toolchainInfo.common.apiVersion
        .hashCode()
    hash = hash * 31 +
      task.info.toolchainInfo.common.languageVersion
        .hashCode()
    hash = hash * 31 +
      task.info.passthroughFlagsList
        .sorted()
        .hashCode()
    hash = hash * 31 +
      task.inputs.compilerPluginOptionsList
        .sorted()
        .hashCode()
    return hash
  }

  private fun storeArgsHash(
    icBaseDir: Path,
    hash: Long,
  ) {
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
  private fun createIcLogger(out: PrintStream): KotlinLogger =
    object : KotlinLogger {
      override val isDebugEnabled: Boolean = true

      override fun error(
        msg: String,
        throwable: Throwable?,
      ) {
        out.println("[IC ERROR] $msg")
        throwable?.printStackTrace(out)
      }

      override fun warn(
        msg: String,
        throwable: Throwable?,
      ) {
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

  /**
   * Compiles Kotlin sources with KAPT (annotation processing) using the Build Tools API.
   *
   * This runs KAPT as a separate compilation phase before main compilation.
   * KAPT generates stubs and processes annotations, producing:
   * - Generated Java sources in directories.generatedJavaSources
   * - Generated classes in directories.generatedClasses
   * - Stubs in directories.stubs
   *
   * @param task The compilation task protobuf containing all compilation info
   * @param plugins Internal compiler plugins (must include kapt)
   * @param aptMode KAPT mode: "stubsAndApt" (default), "stubs", or "apt"
   * @param verbose Whether to enable verbose KAPT output
   * @return CompilationResult indicating success or failure
   */
  fun compileKapt(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
    aptMode: String = "stubsAndApt",
    verbose: Boolean = false,
    out: PrintStream,
  ): CompilationResult {
    // Build KAPT plugin and stubs plugins
    val kaptPlugin = buildKaptCompilerPlugin(task, plugins, aptMode, verbose)
    val stubsPlugins = buildStubsPlugins(task, plugins)
    val compilerPlugins = listOf(kaptPlugin) + stubsPlugins

    val logger = if (verbose) createIcLogger(out) else null

    return executeCompilation(
      task = task,
      outputDir = Path.of(task.directories.generatedClasses),
      compilerPlugins = compilerPlugins,
      logger = logger,
      out = out,
    )
  }

  /**
   * Builds the KAPT compiler plugin using the typed CompilerPlugin API.
   */
  private fun buildKaptCompilerPlugin(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
    aptMode: String,
    verbose: Boolean,
  ): CompilerPlugin {
    val pluginId = plugins.kapt.id

    // Create temp subdirectories for stubs and incremental data
    val stubsDir = Files.createDirectories(Paths.get(task.directories.temp).resolve("stubs"))
    val incrementalDataDir =
      Files.createDirectories(
        Paths.get(task.directories.temp).resolve("incrementalData"),
      )

    val options = mutableListOf<CompilerPluginOption>()

    // Core KAPT directories
    options.add(CompilerPluginOption("sources", task.directories.generatedJavaSources))
    options.add(CompilerPluginOption("classes", task.directories.generatedClasses))
    options.add(CompilerPluginOption("stubs", stubsDir.toString()))
    options.add(CompilerPluginOption("incrementalData", incrementalDataDir.toString()))

    // Javac arguments (encoded as Base64)
    val javacArgs =
      mapOf(
        "-target" to task.info.toolchainInfo.jvm.jvmTarget,
        "-source" to task.info.toolchainInfo.jvm.jvmTarget,
      )
    options.add(CompilerPluginOption("javacArguments", encodeMapForKapt(javacArgs)))

    // Other options
    options.add(CompilerPluginOption("correctErrorTypes", "false"))
    options.add(CompilerPluginOption("verbose", "true")) // verbose.toString()))
    options.add(CompilerPluginOption("aptMode", aptMode))

    // Annotation processor classpath - one option per entry
    task.inputs.processorpathsList.forEach { processorPath ->
      options.add(CompilerPluginOption("apclasspath", processorPath))
    }

    // Processors list - one option per processor
    task.inputs.processorsList.forEach { processor ->
      options.add(CompilerPluginOption("processors", processor))
    }

    // Read kapt apoptions from the plugin options
    val optionPrefix = "$pluginId:apoption="
    val apOptions =
      (task.inputs.compilerPluginOptionsList + task.inputs.stubsPluginOptionsList)
        .filter { o -> o.startsWith(optionPrefix) }
        .associate { o ->
          val kv = o.substring(optionPrefix.length).split(":", limit = 2)
          kv[0] to kv[1]
        }

    if (apOptions.isNotEmpty()) {
      options.add(CompilerPluginOption("apoptions", encodeMapForKapt(apOptions)))
    }

    return CompilerPlugin(
      pluginId = pluginId,
      classpath = listOf(Path.of(plugins.kapt.jarPath)),
      rawArguments = options,
      orderingRequirements = emptySet(),
    )
  }

  /**
   * Builds CompilerPlugin objects for non-KAPT stubs plugins that should run during KAPT phase.
   */
  private fun buildStubsPlugins(
    task: JvmCompilationTask,
    plugins: InternalCompilerPlugins,
  ): List<CompilerPlugin> {
    // Group plugin options by plugin ID
    val pluginOptionsMap = mutableMapOf<String, MutableList<CompilerPluginOption>>()

    task.inputs.stubsPluginOptionsList
      .filterNot { it.startsWith(plugins.kapt.id) }
      .forEach { optionStr ->
        // Format is "pluginId:key=value"
        val colonIdx = optionStr.indexOf(':')
        if (colonIdx > 0) {
          val pluginId = optionStr.substring(0, colonIdx)
          val keyValue = optionStr.substring(colonIdx + 1)
          val eqIdx = keyValue.indexOf('=')
          if (eqIdx > 0) {
            val key = keyValue.substring(0, eqIdx)
            val value = keyValue.substring(eqIdx + 1)
            pluginOptionsMap
              .getOrPut(pluginId) { mutableListOf() }
              .add(CompilerPluginOption(key, value))
          }
        }
      }

    // Create CompilerPlugin for each plugin with options
    // We need to find the classpath for each plugin from stubsPluginClasspathList
    // For simplicity, we map all stubs classpath entries to plugins by matching plugin IDs
    val stubsClasspath = task.inputs.stubsPluginClasspathList.map { Path.of(it) }

    return pluginOptionsMap.map { (pluginId, options) ->
      CompilerPlugin(
        pluginId = pluginId,
        classpath = stubsClasspath, // All stubs plugins share the classpath
        rawArguments = options,
        orderingRequirements = emptySet(),
      )
    }
  }

  /**
   * Encodes a map to Base64 in the format expected by KAPT.
   * Uses Java serialization (ObjectOutputStream) compatible with KAPT's decodeMap.
   */
  private fun encodeMapForKapt(options: Map<String, String>): String {
    val os = ByteArrayOutputStream()
    val oos = ObjectOutputStream(os)

    oos.writeInt(options.size)
    for ((key, value) in options.entries) {
      oos.writeUTF(key)
      oos.writeUTF(value)
    }

    oos.flush()
    return Base64.getEncoder().encodeToString(os.toByteArray())
  }
}
