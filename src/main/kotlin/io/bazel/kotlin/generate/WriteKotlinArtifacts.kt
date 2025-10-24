package io.bazel.kotlin.generate

import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.streams.toList

/**
 * Generates version-specific artifact definitions by introspecting a Kotlin compiler distribution.
 * This allows rules_kotlin to support different Kotlin compiler versions with varying jar structures.
 */
object WriteKotlinArtifacts {

  @JvmStatic
  fun main(vararg args: String) {
    val options = parseArgs(args)

    val kotlinHome = options["--kotlin-home"]?.first()
      ?.run(FileSystems.getDefault()::getPath)
      ?: error("--kotlin-home is required")

    val outputDir = options["--out"]?.first()
      ?.run(FileSystems.getDefault()::getPath)
      ?: error("--out is required")

    val version = options["--version"]?.first()
      ?: error("--version is required (format: X.Y)")

    if (!kotlinHome.exists() || !kotlinHome.isDirectory()) {
      error("Kotlin home directory does not exist: $kotlinHome")
    }

    val libDir = kotlinHome.resolve("lib")
    if (!libDir.exists() || !libDir.isDirectory()) {
      error("Kotlin lib directory does not exist: $libDir")
    }

    // Scan the lib directory
    val artifacts = scanLibDirectory(libDir)

    // Generate artifacts file
    val artifactsFile = outputDir.resolve("artifacts_$version.bzl")
    artifactsFile.writeText(
      generateArtifactsBzl(artifacts, version),
      StandardCharsets.UTF_8
    )

    println("Generated: $artifactsFile")
  }

  private fun parseArgs(args: Array<out String>): Map<String, MutableList<String>> {
    return args.asSequence()
      .flatMap { it.split("=", limit = 2) }
      .chunked(2)
      .fold(mutableMapOf()) { m, (key, value) ->
        m.getOrPut(key) { mutableListOf() }.add(value)
        m
      }
  }

  private fun scanLibDirectory(libDir: Path): KotlinArtifacts {
    val allJars = libDir.listDirectoryEntries("*.jar")
      .filter { it.isRegularFile() }
      .map { it.name }
      .toSet()

    return KotlinArtifacts(
      jvmPlugin = categorizeJars(allJars, JVM_PLUGINS),
      jvmRuntime = categorizeJars(allJars, JVM_RUNTIME),
      jvmCompile = emptyMap(), // JVM compile artifacts are empty
      corePlugin = emptyMap(), // Core plugin artifacts are empty
      coreRuntime = categorizeJars(allJars, CORE_RUNTIME),
      coreCompile = categorizeJars(allJars, CORE_COMPILE)
    )
  }

  private fun categorizeJars(availableJars: Set<String>, knownJars: Map<String, String>): Map<String, String> {
    return knownJars.filterValues { jarName ->
      availableJars.contains(jarName)
    }
  }

  private fun generateArtifactsBzl(artifacts: KotlinArtifacts, version: String): String {
    val stdlibs = artifacts.generateStdlibs()

    return buildString {
      appendLine(HEADER)
      appendLine()
      appendLine("KOTLINC_ARTIFACTS = struct(")
      appendLine("    jvm = struct(")
      appendLine("        plugin = {")
      artifacts.jvmPlugin.forEach { (label, file) ->
        appendLine("            \"$label\": \"lib/$file\",")
      }
      appendLine("        },")
      appendLine("        runtime = {")
      artifacts.jvmRuntime.forEach { (label, file) ->
        appendLine("            \"$label\": \"lib/$file\",")
      }
      appendLine("        },")
      appendLine("        compile = {},")
      appendLine("    ),")
      appendLine("    core = struct(")
      appendLine("        plugin = {},")
      appendLine("        runtime = {")
      artifacts.coreRuntime.forEach { (label, file) ->
        appendLine("            \"$label\": \"lib/$file\",")
      }
      appendLine("        },")
      appendLine("        compile = {")
      artifacts.coreCompile.forEach { (label, file) ->
        appendLine("            \"$label\": \"lib/$file\",")
      }
      appendLine("        },")
      appendLine("    ),")
      appendLine(")")
      appendLine()
      appendLine("KOTLINC_ARTIFACT_LIST = {")
      appendLine("    label: file")
      appendLine("    for lang in [\"jvm\", \"core\"]")
      appendLine("    for type in [\"compile\", \"plugin\", \"runtime\"]")
      appendLine("    for (label, file) in getattr(getattr(KOTLINC_ARTIFACTS, lang), type).items()")
      appendLine("}")
      appendLine()
      appendLine("# List of Kotlin standard library targets for runtime dependencies")
      appendLine("KOTLIN_STDLIBS = [")
      stdlibs.forEach { lib ->
        appendLine("    \"$lib\",")
      }
      appendLine("]")
    }
  }

  private val HEADER = """
# Copyright ${Year.now().value} The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# DO NOT EDIT: generated by bazel run //src/main/kotlin/io/bazel/kotlin/generate:kotlin_artifacts
""".trimIndent()

  // Known JVM plugin artifacts
  private val JVM_PLUGINS = mapOf(
    "allopen-compiler-plugin" to "allopen-compiler-plugin.jar",
    "assignment-compiler-plugin" to "assignment-compiler-plugin.jar",
    "kotlin-imports-dumper-compiler-plugin" to "kotlin-imports-dumper-compiler-plugin.jar",
    "kotlin-serialization-compiler-plugin" to "kotlin-serialization-compiler-plugin.jar",
    "kotlinx-serialization-compiler-plugin" to "kotlinx-serialization-compiler-plugin.jar",
    "lombok-compiler-plugin" to "lombok-compiler-plugin.jar",
    "mutability-annotations-compat" to "mutability-annotations-compat.jar",
    "noarg-compiler-plugin" to "noarg-compiler-plugin.jar",
    "sam-with-receiver-compiler-plugin" to "sam-with-receiver-compiler-plugin.jar",
    "parcelize-compiler-plugin" to "parcelize-compiler.jar",
  )

  // Known JVM runtime artifacts
  private val JVM_RUNTIME = mapOf(
    "jvm-abi-gen" to "jvm-abi-gen.jar",
    "kotlin-stdlib" to "kotlin-stdlib.jar",
    "kotlin-stdlib-jdk7" to "kotlin-stdlib-jdk7.jar",
    "kotlin-stdlib-jdk7-sources" to "kotlin-stdlib-jdk7-sources.jar",
    "kotlin-stdlib-jdk8" to "kotlin-stdlib-jdk8.jar",
    "kotlin-stdlib-jdk8-sources" to "kotlin-stdlib-jdk8-sources.jar",
    "kotlin-stdlib-sources" to "kotlin-stdlib-sources.jar",
    "kotlin-test-junit" to "kotlin-test-junit.jar",
    "kotlin-test-junit-sources" to "kotlin-test-junit-sources.jar",
    "kotlin-test-junit5" to "kotlin-test-junit5.jar",
    "kotlin-test-junit5-sources" to "kotlin-test-junit5-sources.jar",
    "kotlin-test-testng" to "kotlin-test-testng.jar",
    "parcelize-runtime" to "parcelize-runtime.jar",
  )

  // Known core runtime artifacts
  private val CORE_RUNTIME = mapOf(
    "kotlin-reflect" to "kotlin-reflect.jar",
    "kotlin-reflect-sources" to "kotlin-reflect-sources.jar",
    "kotlin-script-runtime" to "kotlin-script-runtime.jar",
    "kotlin-script-runtime-sources" to "kotlin-script-runtime-sources.jar",
    "kotlin-test" to "kotlin-test.jar",
    "kotlin-test-sources" to "kotlin-test-sources.jar",
    "kotlin-test-testng-sources" to "kotlin-test-testng-sources.jar",
    "kotlin-preloader" to "kotlin-preloader.jar",
  )

  // Known core compile artifacts (these may vary by version)
  private val CORE_COMPILE = mapOf(
    "android-extensions-compiler" to "android-extensions-compiler.jar",
    "android-extensions-runtime" to "android-extensions-runtime.jar",
    "annotations" to "annotations-13.0.jar",
    "kotlin-annotation-processing" to "kotlin-annotation-processing.jar",
    "kotlin-annotation-processing-cli" to "kotlin-annotation-processing-cli.jar",
    "kotlin-annotation-processing-compiler" to "kotlin-annotation-processing-compiler.jar",
    "kotlin-annotation-processing-runtime" to "kotlin-annotation-processing-runtime.jar",
    "kotlin-annotations-jvm" to "kotlin-annotations-jvm.jar",
    "kotlin-annotations-jvm-sources" to "kotlin-annotations-jvm-sources.jar",
    "kotlin-compiler" to "kotlin-compiler.jar",
    "kotlin-daemon" to "kotlin-daemon.jar",
    "kotlin-daemon-client" to "kotlin-daemon-client.jar",
    "kotlin-main-kts" to "kotlin-main-kts.jar",
    "kotlin-runner" to "kotlin-runner.jar",
    "kotlin-scripting-common" to "kotlin-scripting-common.jar",
    "kotlin-scripting-compiler" to "kotlin-scripting-compiler.jar",
    "kotlin-scripting-compiler-impl" to "kotlin-scripting-compiler-impl.jar",
    "kotlin-scripting-jvm" to "kotlin-scripting-jvm.jar",
    "kotlinx-coroutines-core-jvm" to "kotlinx-coroutines-core-jvm.jar",
    "parcelize-compiler" to "parcelize-compiler.jar",
    "scripting-compiler" to "scripting-compiler.jar",
    "trove4j" to "trove4j.jar", // This is the jar that was removed in Kotlin 2.2
  )

  private data class KotlinArtifacts(
    val jvmPlugin: Map<String, String>,
    val jvmRuntime: Map<String, String>,
    val jvmCompile: Map<String, String>,
    val corePlugin: Map<String, String>,
    val coreRuntime: Map<String, String>,
    val coreCompile: Map<String, String>,
  ) {
    fun generateStdlibs(): List<String> {
      val stdlibs = mutableListOf<String>()

      // Always include annotations if available
      if (coreCompile.containsKey("annotations")) {
        stdlibs.add("//kotlin/compiler:annotations")
      }

      // Add stdlib variants
      if (jvmRuntime.containsKey("kotlin-stdlib")) {
        stdlibs.add("//kotlin/compiler:kotlin-stdlib")
      }
      if (jvmRuntime.containsKey("kotlin-stdlib-jdk7")) {
        stdlibs.add("//kotlin/compiler:kotlin-stdlib-jdk7")
      }
      if (jvmRuntime.containsKey("kotlin-stdlib-jdk8")) {
        stdlibs.add("//kotlin/compiler:kotlin-stdlib-jdk8")
      }

      // Add coroutines if available
      if (coreCompile.containsKey("kotlinx-coroutines-core-jvm")) {
        stdlibs.add("//kotlin/compiler:kotlinx-coroutines-core-jvm")
      }

      // Add trove4j only if it exists (Kotlin <2.2)
      if (coreCompile.containsKey("trove4j")) {
        stdlibs.add("//kotlin/compiler:trove4j")
      }

      return stdlibs
    }
  }
}
