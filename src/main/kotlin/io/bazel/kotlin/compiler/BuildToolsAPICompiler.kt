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

import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.PrintStream
import java.nio.file.Path

/**
 * Compiler that uses the Kotlin Build Tools API.
 *
 * All Build Tools API classes are loaded via reflection to avoid classloading issues.
 * The Build Tools API classes are in separate jars that are loaded by the toolchain
 * classloader, not the system classloader that loads this class.
 */
@Suppress("unused")
class BuildToolsAPICompiler {
  fun exec(
    errStream: PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    // Parse source files and output directory from arguments
    // Also filter out custom incremental compilation flags that Build Tools API doesn't understand
    val argsList = args.toList()
    val filteredArgs = mutableListOf<String>()
    val sourceFiles = mutableListOf<Path>()
    var outputDir = Path.of(".")
    var i = 0
    while (i < argsList.size) {
      val arg = argsList[i]
      when {
        arg == "-d" && i + 1 < argsList.size -> {
          outputDir = Path.of(argsList[i + 1])
          filteredArgs.add(arg)
          filteredArgs.add(argsList[i + 1])
          i += 2
        }
        // Skip custom incremental compilation flags (they have values after them)
        arg in listOf("-incremental_id", "-incremental_dir", "-snapshot") && i + 1 < argsList.size -> {
          i += 2
        }
        !arg.startsWith("-") && (arg.endsWith(".kt") || arg.endsWith(".java")) -> {
          sourceFiles.add(Path.of(arg))
          filteredArgs.add(arg)
          i++
        }
        else -> {
          filteredArgs.add(arg)
          i++
        }
      }
    }

    val classLoader = this.javaClass.classLoader!!

    // Load Build Tools API classes via reflection to avoid classloader issues
    val kotlinToolchainsClass = classLoader.loadClass("org.jetbrains.kotlin.buildtools.api.KotlinToolchains")
    val loadImplementationMethod = kotlinToolchainsClass.getMethod("loadImplementation", ClassLoader::class.java)
    val kotlinToolchains = loadImplementationMethod.invoke(null, classLoader)

    // Get JVM platform toolchain via the companion extension
    val kotlinToolchainsInterface = classLoader.loadClass("org.jetbrains.kotlin.buildtools.api.ToolchainsProvider")
    val jvmToolchainClass = classLoader.loadClass("org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain")
    val companionClass = classLoader.loadClass("org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain\$Companion")
    val companionInstance = jvmToolchainClass.getField("Companion").get(null)
    val getJvmMethod = companionClass.getMethod("getJvm", kotlinToolchainsInterface)
    val jvmToolchain = getJvmMethod.invoke(companionInstance, kotlinToolchains)

    // Create compilation operation
    val createOperationMethod = jvmToolchain.javaClass.getMethod(
      "createJvmCompilationOperation",
      List::class.java,
      Path::class.java,
    )
    val operation = createOperationMethod.invoke(jvmToolchain, sourceFiles, outputDir)

    // Apply arguments
    val compilerArgumentsProperty = operation.javaClass.getMethod("getCompilerArguments").invoke(operation)
    val applyArgsMethod = compilerArgumentsProperty.javaClass.getMethod("applyArgumentStrings", List::class.java)
    applyArgsMethod.invoke(compilerArgumentsProperty, filteredArgs)

    // Execute the compilation
    val oldOut = System.out
    val oldErr = System.err
    System.setOut(errStream)
    System.setErr(errStream)
    val result =
      try {
        val createSessionMethod = kotlinToolchains.javaClass.getMethod("createBuildSession")
        val session = createSessionMethod.invoke(kotlinToolchains)
        try {
          val operationInterface = classLoader.loadClass("org.jetbrains.kotlin.buildtools.api.CompilationOperation")
          val executeMethod = session.javaClass.getMethod("executeOperation", operationInterface)
          executeMethod.invoke(session, operation)
        } finally {
          // Close the session (it's AutoCloseable)
          val closeMethod = session.javaClass.getMethod("close")
          closeMethod.invoke(session)
        }
      } finally {
        System.setOut(oldOut)
        System.setErr(oldErr)
      }

    // Map CompilationResult to ExitCode
    val resultName = result.javaClass.getMethod("name").invoke(result) as String
    return when (resultName) {
      "COMPILATION_SUCCESS" -> ExitCode.OK
      "COMPILATION_ERROR" -> ExitCode.COMPILATION_ERROR
      "COMPILATION_OOM_ERROR" -> ExitCode.OOM_ERROR
      "COMPILER_INTERNAL_ERROR" -> ExitCode.INTERNAL_ERROR
      else -> ExitCode.INTERNAL_ERROR
    }
  }
}
