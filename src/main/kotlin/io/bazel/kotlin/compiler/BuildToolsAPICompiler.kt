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
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.cli.common.ExitCode
import java.util.UUID

@Suppress("unused")
class BuildToolsAPICompiler {
  @OptIn(ExperimentalBuildToolsApi::class)
  fun exec(
    errStream: java.io.PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    val kotlinService = CompilationService.loadImplementation(this.javaClass.classLoader!!)
    // The execution configuration. Controls in-process vs daemon execution strategies. Default is in-process.
    val executionConfig = kotlinService.makeCompilerExecutionStrategyConfiguration()
    // The compilation configuration. Controls everything related to incremental compilation.
    val compilationConfig =
      kotlinService.makeJvmCompilationConfiguration().apply {
        // useLogger(BasicKotlinLogger(true, "/tmp/kotlin_log/$label.log"))
      }

    // Redirect System.err to our errStream during compilation to capture error output.
    val originalErr = System.err
    System.setErr(errStream)
    val result =
      try {
        kotlinService.compileJvm(
          ProjectId.ProjectUUID(UUID.randomUUID()),
          executionConfig,
          compilationConfig,
          emptyList(),
          args.toList(),
        )
      } finally {
        System.setErr(originalErr)
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
