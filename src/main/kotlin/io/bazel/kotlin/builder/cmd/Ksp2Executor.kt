/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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

package io.bazel.kotlin.builder.cmd

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.ArgMaps
import io.bazel.kotlin.builder.utils.Flag
import io.bazel.worker.WorkerContext
import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.ServiceLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executor for KSP2 (Kotlin Symbol Processing) that uses the KSP2 programmatic API
 * and enables it to run as a Bazel persistent worker.
 */
@Singleton
class Ksp2Executor
  @Inject
  constructor() {
    companion object {
      @JvmStatic
      private val FLAGFILE_RE = Regex("""^--flagfile=((.*)-(\d+).params)$""")

      /**
       * Flags used by the KSP2 builder.
       */
      enum class Ksp2Flags(
        override val flag: String,
      ) : Flag {
        JVM_TARGET("--jvm_target"),
        MODULE_NAME("--module_name"),
        SOURCE_ROOTS("--source_roots"),
        JAVA_SOURCE_ROOTS("--java_source_roots"),
        PROJECT_BASE_DIR("--project_base_dir"),
        OUTPUT_BASE_DIR("--output_base_dir"),
        CACHES_DIR("--caches_dir"),
        CLASS_OUTPUT_DIR("--class_output_dir"),
        KOTLIN_OUTPUT_DIR("--kotlin_output_dir"),
        JAVA_OUTPUT_DIR("--java_output_dir"),
        RESOURCE_OUTPUT_DIR("--resource_output_dir"),
        LANGUAGE_VERSION("--language_version"),
        API_VERSION("--api_version"),
        JDK_HOME("--jdk_home"),
        LIBRARIES("--libraries"),
        PROCESSOR_CLASSPATH("--processor_classpath"),
      }
    }

    private fun getArgs(args: List<String>): ArgMap {
      check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
      val lines =
        FLAGFILE_RE.matchEntire(args[0])?.groups?.get(1)?.let {
          Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
        } ?: args

      return ArgMaps.from(lines)
    }

    fun execute(
      ctx: WorkerContext.TaskContext,
      args: List<String>,
    ): Int {
      val argMap = getArgs(args)

      return try {
        // Load processors from the processor classpath
        val processorClasspath = argMap.optional(Ksp2Flags.PROCESSOR_CLASSPATH) ?: emptyList()
        val processorUrls = processorClasspath.map { File(it).toURI().toURL() }.toTypedArray()
        val processorClassLoader = URLClassLoader(processorUrls, this.javaClass.classLoader)

        @Suppress("UNCHECKED_CAST")
        val processors = ServiceLoader.load(
          processorClassLoader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider"),
          processorClassLoader,
        ).toList() as List<SymbolProcessorProvider>

        // Build KSP config
        val config = KSPJvmConfig.Builder().apply {
          moduleName = argMap.mandatorySingle(Ksp2Flags.MODULE_NAME)

          // Source roots
          argMap.optionalSingle(Ksp2Flags.SOURCE_ROOTS)?.let { roots ->
            sourceRoots = roots.split(":").filter { it.isNotEmpty() }.map { File(it) }
          }

          // Java source roots
          argMap.optionalSingle(Ksp2Flags.JAVA_SOURCE_ROOTS)?.let { roots ->
            javaSourceRoots = roots.split(":").filter { it.isNotEmpty() }.map { File(it) }
          }

          // Libraries (classpath)
          argMap.optionalSingle(Ksp2Flags.LIBRARIES)?.let { libs ->
            libraries = libs.split(":").filter { it.isNotEmpty() }.map { File(it) }
          }

          // Output directories
          argMap.optionalSingle(Ksp2Flags.KOTLIN_OUTPUT_DIR)?.let { kotlinOutputDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.JAVA_OUTPUT_DIR)?.let { javaOutputDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.CLASS_OUTPUT_DIR)?.let { classOutputDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.RESOURCE_OUTPUT_DIR)?.let { resourceOutputDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.CACHES_DIR)?.let { cachesDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.PROJECT_BASE_DIR)?.let { projectBaseDir = File(it) }
          argMap.optionalSingle(Ksp2Flags.OUTPUT_BASE_DIR)?.let { outputBaseDir = File(it) }

          // Kotlin/JVM settings
          argMap.optionalSingle(Ksp2Flags.JVM_TARGET)?.let { jvmTarget = it }
          argMap.optionalSingle(Ksp2Flags.LANGUAGE_VERSION)?.let { languageVersion = it }
          argMap.optionalSingle(Ksp2Flags.API_VERSION)?.let { apiVersion = it }
          argMap.optionalSingle(Ksp2Flags.JDK_HOME)?.let { jdkHome = File(it) }

          // Enable Java annotation argument mapping
          mapAnnotationArgumentsInJava = true
        }.build()

        // Create logger
        val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)

        // Execute KSP
        val exitCode = KotlinSymbolProcessing(config, processors, logger).execute()

        ctx.info { "KSP2 completed with exit code: $exitCode" }
        exitCode.code
      } catch (e: Exception) {
        ctx.error(e) { "KSP2 execution failed" }
        1
      }
    }
  }
