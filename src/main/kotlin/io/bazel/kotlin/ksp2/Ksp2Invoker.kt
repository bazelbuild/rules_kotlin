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
    val processors = loadProcessors(classLoader)
    val kspConfig = buildJvmConfig(request)
    val logger = KspGradleLogger(request.requireInt("logLevel"))
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

  private fun loadProcessors(classLoader: ClassLoader): List<SymbolProcessorProvider> =
    ServiceLoader.load(SymbolProcessorProvider::class.java, classLoader).toList()

  private fun buildJvmConfig(request: Map<String, Any?>): KSPJvmConfig =
    KSPJvmConfig
      .Builder()
      .apply {
        moduleName = request.requireString("moduleName")
        sourceRoots = request.requireStringList("sourceRoots").map(::File)
        javaSourceRoots = request.requireStringList("javaSourceRoots").map(::File)
        libraries = request.requireStringList("libraries").map(::File)
        kotlinOutputDir = File(request.requireString("kotlinOutputDir"))
        javaOutputDir = File(request.requireString("javaOutputDir"))
        classOutputDir = File(request.requireString("classOutputDir"))
        resourceOutputDir = File(request.requireString("resourceOutputDir"))
        cachesDir = File(request.requireString("cachesDir"))
        projectBaseDir = File(request.requireString("projectBaseDir"))
        outputBaseDir = File(request.requireString("outputBaseDir"))
        mapAnnotationArgumentsInJava = true

        request.optionalString("jvmTarget")?.let { jvmTarget = it }
        request.optionalString("languageVersion")?.let { languageVersion = it }
        request.optionalString("apiVersion")?.let { apiVersion = it }
        request.optionalString("jdkHome")?.let { jdkHome = File(it) }
      }.build()
}
