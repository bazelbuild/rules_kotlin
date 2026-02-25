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
import io.bazel.kotlin.builder.tasks.jvm.Ksp2EntryPoint
import io.bazel.kotlin.builder.tasks.jvm.Ksp2Request
import java.util.ServiceLoader

/**
 * Wrapper for KSP2 invocation using direct API calls.
 *
 * This class is compiled against KSP2 classes (via neverlink_deps) and loaded
 * at runtime in a classloader that has the KSP2 jars.
 */
class Ksp2Invoker(
  private val classLoader: ClassLoader,
) : Ksp2EntryPoint {
  /**
   * Execute KSP2 with the given configuration.
   *
   * @return Exit code (0 for success)
   */
  override fun execute(request: Ksp2Request): Int {
    // Load processors via ServiceLoader from the provided classloader
    val processors =
      ServiceLoader.load(SymbolProcessorProvider::class.java, classLoader).toList()

    // Build KSP2 configuration
    val kspConfig =
      KSPJvmConfig
        .Builder()
        .apply {
          this.moduleName = request.moduleName
          this.sourceRoots = request.sourceRoots
          this.javaSourceRoots = request.javaSourceRoots
          this.libraries = request.libraries
          this.kotlinOutputDir = request.kotlinOutputDir
          this.javaOutputDir = request.javaOutputDir
          this.classOutputDir = request.classOutputDir
          this.resourceOutputDir = request.resourceOutputDir
          this.cachesDir = request.cachesDir
          this.projectBaseDir = request.projectBaseDir
          this.outputBaseDir = request.outputBaseDir
          request.jvmTarget?.let { this.jvmTarget = it }
          request.languageVersion?.let { this.languageVersion = it }
          request.apiVersion?.let { this.apiVersion = it }
          request.jdkHome?.let { this.jdkHome = it }
          this.mapAnnotationArgumentsInJava = true
        }.build()

    // Create logger and execute
    val logger = KspGradleLogger(request.logLevel)
    val ksp = KotlinSymbolProcessing(kspConfig, processors, logger)

    return ksp.execute().code
  }
}
