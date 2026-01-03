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
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.cli.common.ExitCode
import java.nio.file.Path

@Suppress("unused")
class BuildToolsAPICompiler {
  @OptIn(ExperimentalBuildToolsApi::class)
  fun exec(
    errStream: java.io.PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    // Parse source files and output directory from arguments
    // Source files are positional arguments (don't start with -)
    // Output directory comes after -d flag
    val argsList = args.toList()
    val sourceFiles = mutableListOf<Path>()
    var outputDir = Path.of(".")
    var i = 0
    while (i < argsList.size) {
      val arg = argsList[i]
      when {
        arg == "-d" && i + 1 < argsList.size -> {
          outputDir = Path.of(argsList[i + 1])
          i += 2
        }
        !arg.startsWith("-") && (arg.endsWith(".kt") || arg.endsWith(".java")) -> {
          sourceFiles.add(Path.of(arg))
          i++
        }
        else -> i++
      }
    }

    val kotlinToolchains = KotlinToolchains.loadImplementation(this.javaClass.classLoader!!)

    // Create compilation operation with parsed sources and output directory
    val operation =
      kotlinToolchains.jvm.createJvmCompilationOperation(
        sourceFiles,
        outputDir,
      )

    // Apply raw CLI arguments - this handles flags like -cp, -api-version, etc.
    operation.compilerArguments.applyArgumentStrings(argsList)

    // Execute the compilation, redirecting stdout/stderr to the provided stream
    val oldOut = System.out
    val oldErr = System.err
    System.setOut(errStream)
    System.setErr(errStream)
    val result =
      try {
        kotlinToolchains.createBuildSession().use { session ->
          session.executeOperation(operation)
        }
      } finally {
        System.setOut(oldOut)
        System.setErr(oldErr)
      }

    // BTAPI returns a different type than K2JVMCompiler (CompilationResult vs ExitCode).
    return when (result) {
      CompilationResult.COMPILATION_SUCCESS -> ExitCode.OK
      CompilationResult.COMPILATION_ERROR -> ExitCode.COMPILATION_ERROR
      CompilationResult.COMPILATION_OOM_ERROR -> ExitCode.OOM_ERROR
      CompilationResult.COMPILER_INTERNAL_ERROR -> ExitCode.INTERNAL_ERROR
    }
  }
}
