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
package io.bazel.kotlin.ksp2

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import java.util.ServiceLoader

/**
 * Isolated wrapper for KSP2 invocation.
 *
 * Loaded by a dedicated classloader and invoked reflectively from the worker.
 * The request uses only JDK types to avoid sharing custom classes between loaders.
 */
object Ksp2Invoker {
  @JvmStatic
  fun execute(
    classLoader: ClassLoader,
    request: Map<String, Any?>,
  ): Int {
    // Load processors via ServiceLoader from the provided classloader
    val processors =
      ServiceLoader.load(SymbolProcessorProvider::class.java, classLoader).toList()

    // Build KSP2 configuration
    val moduleName = request.requireString("moduleName")
    val sourceRoots = request.requireStringList("sourceRoots").map(::File)
    val javaSourceRoots = request.requireStringList("javaSourceRoots").map(::File)
    val libraries = request.requireStringList("libraries").map(::File)
    val kotlinOutputDir = File(request.requireString("kotlinOutputDir"))
    val javaOutputDir = File(request.requireString("javaOutputDir"))
    val classOutputDir = File(request.requireString("classOutputDir"))
    val resourceOutputDir = File(request.requireString("resourceOutputDir"))
    val cachesDir = File(request.requireString("cachesDir"))
    val projectBaseDir = File(request.requireString("projectBaseDir"))
    val outputBaseDir = File(request.requireString("outputBaseDir"))
    val jvmTarget = request.optionalString("jvmTarget")
    val languageVersion = request.optionalString("languageVersion")
    val apiVersion = request.optionalString("apiVersion")
    val jdkHome = request.optionalString("jdkHome")?.let(::File)
    val logLevel = request.requireInt("logLevel")

    val kspConfig =
      KSPJvmConfig
        .Builder()
        .apply {
          this.moduleName = moduleName
          this.sourceRoots = sourceRoots
          this.javaSourceRoots = javaSourceRoots
          this.libraries = libraries
          this.kotlinOutputDir = kotlinOutputDir
          this.javaOutputDir = javaOutputDir
          this.classOutputDir = classOutputDir
          this.resourceOutputDir = resourceOutputDir
          this.cachesDir = cachesDir
          this.projectBaseDir = projectBaseDir
          this.outputBaseDir = outputBaseDir
          jvmTarget?.let { this.jvmTarget = it }
          languageVersion?.let { this.languageVersion = it }
          apiVersion?.let { this.apiVersion = it }
          jdkHome?.let { this.jdkHome = it }
          this.mapAnnotationArgumentsInJava = true
        }.build()

    // Create logger and execute
    val logger = KspGradleLogger(logLevel)
    val ksp = KotlinSymbolProcessing(kspConfig, processors, logger)

    return ksp.execute().code
  }

  private fun Map<String, Any?>.optionalString(key: String): String? = this[key] as? String

  private fun Map<String, Any?>.requireString(key: String): String =
    optionalString(key) ?: error("Missing KSP2 request value: $key")

  private fun Map<String, Any?>.requireStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map {
      it as? String ?: error("KSP2 request value '$key' must be List<String>")
    } ?: error("Missing KSP2 request value: $key")

  private fun Map<String, Any?>.requireInt(key: String): Int =
    (this[key] as? Number)?.toInt() ?: error("Missing KSP2 request value: $key")
}
