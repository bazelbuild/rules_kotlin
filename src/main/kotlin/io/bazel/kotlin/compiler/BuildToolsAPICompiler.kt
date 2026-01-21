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

    // Parse args to extract source files, destination, and IC configuration
    val argsList = args.toList()
    var destination: Path = Path.of(".")
    val sources = mutableListOf<Path>()

    // IC configuration arguments
    var icWorkingDir: Path? = null
    var icClasspathSnapshots: List<Path> = emptyList()
    var icShrunkSnapshot: Path? = null
    var icRootProjectDir: Path? = null
    var icForceRecompilation = false
    var icEnableLogging = false

    var i = 0
    while (i < argsList.size) {
      val arg = argsList[i]
      when {
        arg == "-d" && i + 1 < argsList.size -> {
          destination = Path.of(argsList[i + 1])
          i += 2
        }
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
        !arg.startsWith("-") && (arg.endsWith(".kt") || arg.endsWith(".java")) -> {
          sources.add(Path.of(arg))
          i++
        }
        else -> i++
      }
    }

    val kotlinToolchains = KotlinToolchains.loadImplementation(this.javaClass.classLoader!!)

    val operation = kotlinToolchains.jvm.createJvmCompilationOperation(sources, destination)

    // Apply all CLI arguments (excluding IC-specific ones which are handled above)
    val compilerArgs = argsList.filter { !it.startsWith("--ic-") }
    operation.compilerArguments.applyArgumentStrings(compilerArgs)

    // Configure incremental compilation if working directory is provided
    if (icWorkingDir != null) {
      // Derive shrunk snapshot path from working directory if not explicitly provided
      val shrunkSnapshot = icShrunkSnapshot ?: icWorkingDir.resolve("shrunk-classpath-snapshot.bin")

      val icOptions =
        operation.createSnapshotBasedIcOptions().apply {
          icRootProjectDir?.let { this[ROOT_PROJECT_DIR] = it }
          this[MODULE_BUILD_DIR] = destination.parent ?: destination
          this[FORCE_RECOMPILATION] = icForceRecompilation
          // OUTPUT_DIRS should include both destination and working directory
          this[OUTPUT_DIRS] = setOf(destination, icWorkingDir)
        }

      operation[INCREMENTAL_COMPILATION] =
        JvmSnapshotBasedIncrementalCompilationConfiguration(
          workingDirectory = icWorkingDir,
          sourcesChanges = SourcesChanges.ToBeCalculated,
          dependenciesSnapshotFiles = icClasspathSnapshots,
          shrunkClasspathSnapshot = shrunkSnapshot,
          options = icOptions,
        )
    }

    val logger = if (icEnableLogging) ICLogger(errStream) else null

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
}
