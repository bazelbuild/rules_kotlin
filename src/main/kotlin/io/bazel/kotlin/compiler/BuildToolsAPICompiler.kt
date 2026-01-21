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
package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
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
import org.jetbrains.kotlin.cli.common.ExitCode
import java.nio.file.Path

/**
 * Parsed compiler arguments extracted from CLI arguments.
 * These are the arguments we handle with typed BTAPI setters.
 *
 * Only enum-typed args (JVM_TARGET, API_VERSION, LANGUAGE_VERSION) use typed setters
 * for type safety. Other args pass through as strings for IC compatibility.
 */
private data class ParsedArgs(
  val destination: Path,
  val sources: List<Path>,
  // Typed args (use BTAPI enum setters)
  val jvmTarget: String?,
  val apiVersion: String?,
  val languageVersion: String?,
  // IC configuration
  val icWorkingDir: Path?,
  val icClasspathSnapshots: List<Path>,
  val icShrunkSnapshot: Path?,
  val icRootProjectDir: Path?,
  val icForceRecompilation: Boolean,
  val icEnableLogging: Boolean,
  // Remaining args that pass through to applyArgumentStrings
  val passthroughArgs: List<String>,
)

/**
 * Compiler that uses the Kotlin Build Tools API.
 */
@Suppress("unused")
class BuildToolsAPICompiler {
  @OptIn(ExperimentalBuildToolsApi::class)
  fun exec(
    errStream: java.io.PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    // Redirect stderr to capture compiler error messages
    val originalErr = System.err
    System.setErr(errStream)

    // Parse args into typed structure
    val parsedArgs = parseArgs(args.toList())

    val kotlinToolchains = KotlinToolchains.loadImplementation(this.javaClass.classLoader!!)

    val operation = kotlinToolchains.jvm.createJvmCompilationOperation(parsedArgs.sources, parsedArgs.destination)
    val compilerArgs = operation.compilerArguments

    // Use typed BTAPI setters for enum-typed arguments (provides type safety)
    parsedArgs.jvmTarget?.let { target ->
      parseJvmTarget(target)?.let { compilerArgs[JvmCompilerArguments.JVM_TARGET] = it }
    }
    parsedArgs.apiVersion?.let { version ->
      parseKotlinVersion(version)?.let { compilerArgs[CommonCompilerArguments.API_VERSION] = it }
    }
    parsedArgs.languageVersion?.let { version ->
      parseKotlinVersion(version)?.let { compilerArgs[CommonCompilerArguments.LANGUAGE_VERSION] = it }
    }

    // Apply all other args via passthrough (including module name, classpath, plugins)
    if (parsedArgs.passthroughArgs.isNotEmpty()) {
      compilerArgs.applyArgumentStrings(parsedArgs.passthroughArgs)
    }

    // Configure incremental compilation if working directory is provided
    if (parsedArgs.icWorkingDir != null) {
      val icWorkingDir = parsedArgs.icWorkingDir
      // Derive shrunk snapshot path from working directory if not explicitly provided
      val shrunkSnapshot = parsedArgs.icShrunkSnapshot ?: icWorkingDir.resolve("shrunk-classpath-snapshot.bin")

      val icOptions =
        operation.createSnapshotBasedIcOptions().apply {
          parsedArgs.icRootProjectDir?.let { this[ROOT_PROJECT_DIR] = it }
          this[MODULE_BUILD_DIR] = parsedArgs.destination.parent ?: parsedArgs.destination
          this[FORCE_RECOMPILATION] = parsedArgs.icForceRecompilation
          // OUTPUT_DIRS should include both destination and working directory
          this[OUTPUT_DIRS] = setOf(parsedArgs.destination, icWorkingDir)
        }

      operation[INCREMENTAL_COMPILATION] =
        JvmSnapshotBasedIncrementalCompilationConfiguration(
          workingDirectory = icWorkingDir,
          sourcesChanges = SourcesChanges.ToBeCalculated,
          dependenciesSnapshotFiles = parsedArgs.icClasspathSnapshots,
          shrunkClasspathSnapshot = shrunkSnapshot,
          options = icOptions,
        )
    }

    val logger = if (parsedArgs.icEnableLogging) ICLogger(errStream) else null

    // Execute the compilation
    try {
      val result =
        kotlinToolchains.createBuildSession().use { session ->
          session.executeOperation(operation, logger = logger)
        }

      return when (result) {
        CompilationResult.COMPILATION_SUCCESS -> ExitCode.OK
        CompilationResult.COMPILATION_ERROR -> ExitCode.COMPILATION_ERROR
        CompilationResult.COMPILATION_OOM_ERROR -> ExitCode.OOM_ERROR
        CompilationResult.COMPILER_INTERNAL_ERROR -> ExitCode.INTERNAL_ERROR
      }
    } finally {
      System.setErr(originalErr)
    }
  }

  /**
   * Parse CLI arguments into a typed structure.
   * Known arguments are extracted for typed BTAPI setters.
   * Unknown arguments are collected for passthrough to applyArgumentStrings.
   */
  private fun parseArgs(argsList: List<String>): ParsedArgs {
    var destination: Path = Path.of(".")
    val sources = mutableListOf<Path>()
    var jvmTarget: String? = null
    var apiVersion: String? = null
    var languageVersion: String? = null

    // IC configuration arguments
    var icWorkingDir: Path? = null
    var icClasspathSnapshots: List<Path> = emptyList()
    var icShrunkSnapshot: Path? = null
    var icRootProjectDir: Path? = null
    var icForceRecompilation = false
    var icEnableLogging = false

    // Passthrough args (plugins and other flags without typed setters)
    val passthroughArgs = mutableListOf<String>()

    var i = 0
    while (i < argsList.size) {
      val arg = argsList[i]
      when {
        // Destination
        arg == "-d" && i + 1 < argsList.size -> {
          destination = Path.of(argsList[i + 1])
          i += 2
        }
        // JVM target
        arg == "-jvm-target" && i + 1 < argsList.size -> {
          jvmTarget = argsList[i + 1]
          i += 2
        }
        // API version
        arg == "-api-version" && i + 1 < argsList.size -> {
          apiVersion = argsList[i + 1]
          i += 2
        }
        // Language version
        arg == "-language-version" && i + 1 < argsList.size -> {
          languageVersion = argsList[i + 1]
          i += 2
        }
        // Module name - passthrough for IC compatibility
        // Classpath - passthrough (string type, no benefit from typed setter)
        // Friend paths - passthrough (experimental, complex format)
        // IC arguments
        arg.startsWith("--ic-working-dir=") -> {
          icWorkingDir = Path.of(arg.substringAfter("="))
          i++
        }
        arg.startsWith("--ic-classpath-snapshots=") -> {
          val snapshotsStr = arg.substringAfter("=")
          icClasspathSnapshots =
            if (snapshotsStr.isNotEmpty()) {
              snapshotsStr.split(",").map { Path.of(it) }
            } else {
              emptyList()
            }
          i++
        }
        arg.startsWith("--ic-shrunk-snapshot=") -> {
          icShrunkSnapshot = Path.of(arg.substringAfter("="))
          i++
        }
        arg.startsWith("--ic-root-project-dir=") -> {
          icRootProjectDir = Path.of(arg.substringAfter("="))
          i++
        }
        arg == "--ic-force-recompilation" -> {
          icForceRecompilation = true
          i++
        }
        arg == "--ic-enable-logging" -> {
          icEnableLogging = true
          i++
        }
        // Source files
        !arg.startsWith("-") && (arg.endsWith(".kt") || arg.endsWith(".java")) -> {
          sources.add(Path.of(arg))
          i++
        }
        // Unknown args go to passthrough (includes plugins)
        else -> {
          passthroughArgs.add(arg)
          i++
        }
      }
    }

    return ParsedArgs(
      destination = destination,
      sources = sources,
      jvmTarget = jvmTarget,
      apiVersion = apiVersion,
      languageVersion = languageVersion,
      icWorkingDir = icWorkingDir,
      icClasspathSnapshots = icClasspathSnapshots,
      icShrunkSnapshot = icShrunkSnapshot,
      icRootProjectDir = icRootProjectDir,
      icForceRecompilation = icForceRecompilation,
      icEnableLogging = icEnableLogging,
      passthroughArgs = passthroughArgs,
    )
  }

  /**
   * Parse JVM target string to BTAPI enum.
   * Returns null if the target is not recognized (will fall through to passthrough).
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
   * Returns null if the version is not recognized (will fall through to passthrough).
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
}
