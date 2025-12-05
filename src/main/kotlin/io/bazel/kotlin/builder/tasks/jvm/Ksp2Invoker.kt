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
package io.bazel.kotlin.builder.tasks.jvm

import java.io.File
import java.lang.reflect.Method
import java.util.ServiceLoader

/**
 * Wrapper for KSP2 invocation via reflection.
 *
 * This class loads all necessary KSP2 classes and methods once during construction,
 * avoiding repeated reflection overhead on each invocation. This follows the same
 * pattern as KotlincInvoker and BuildToolsAPICompiler.
 */
class Ksp2Invoker(
  private val classLoader: ClassLoader,
) {
  // KSP2 classes - loaded once
  private val symbolProcessorProviderClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider")
  private val kspJvmConfigBuilderClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.processing.KSPJvmConfig\$Builder")
  private val kspGradleLoggerClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.processing.KspGradleLogger")
  private val kotlinSymbolProcessingClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.impl.KotlinSymbolProcessing")
  private val kspConfigClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.processing.KSPConfig")
  private val kspLoggerClass: Class<*> =
    classLoader.loadClass("com.google.devtools.ksp.processing.KSPLogger")

  // Methods - looked up once
  private val configBuilderConstructor = kspJvmConfigBuilderClass.getConstructor()
  private val buildMethod: Method = kspJvmConfigBuilderClass.getMethod("build")
  private val loggerConstructor = kspGradleLoggerClass.getConstructor(Int::class.java)
  private val kspConstructor =
    kotlinSymbolProcessingClass.getConstructor(
      kspConfigClass,
      List::class.java,
      kspLoggerClass,
    )
  private val executeMethod: Method = kotlinSymbolProcessingClass.getMethod("execute")

  // Config builder setters - cached by name
  private val configSetters: Map<String, Method> =
    kspJvmConfigBuilderClass.methods
      .filter { it.name.startsWith("set") }
      .associateBy { it.name }

  /**
   * Configuration for KSP2 execution.
   */
  data class Ksp2Config(
    val moduleName: String,
    val sourceRoots: List<File>,
    val javaSourceRoots: List<File> = emptyList(),
    val libraries: List<File> = emptyList(),
    val kotlinOutputDir: File,
    val javaOutputDir: File,
    val classOutputDir: File,
    val resourceOutputDir: File,
    val cachesDir: File,
    val projectBaseDir: File,
    val outputBaseDir: File,
    val jvmTarget: String? = null,
    val languageVersion: String? = null,
    val apiVersion: String? = null,
    val jdkHome: File? = null,
  )

  /**
   * Execute KSP2 with the given configuration.
   *
   * @param config KSP2 configuration
   * @param logLevel Logger level (0=ERROR, 1=WARN, 2=INFO, 3=LOGGING)
   * @return Exit code (0 for success)
   */
  fun execute(
    config: Ksp2Config,
    logLevel: Int = 1,
  ): Int {
    // Load processors via ServiceLoader
    val processors =
      ServiceLoader.load(symbolProcessorProviderClass, classLoader).toList()

    // Build KSP2 configuration
    val configBuilder = configBuilderConstructor.newInstance()

    setProperty(configBuilder, "moduleName", config.moduleName)
    setProperty(configBuilder, "sourceRoots", config.sourceRoots)
    if (config.javaSourceRoots.isNotEmpty()) {
      setProperty(configBuilder, "javaSourceRoots", config.javaSourceRoots)
    }
    if (config.libraries.isNotEmpty()) {
      setProperty(configBuilder, "libraries", config.libraries)
    }
    setProperty(configBuilder, "kotlinOutputDir", config.kotlinOutputDir)
    setProperty(configBuilder, "javaOutputDir", config.javaOutputDir)
    setProperty(configBuilder, "classOutputDir", config.classOutputDir)
    setProperty(configBuilder, "resourceOutputDir", config.resourceOutputDir)
    setProperty(configBuilder, "cachesDir", config.cachesDir)
    setProperty(configBuilder, "projectBaseDir", config.projectBaseDir)
    setProperty(configBuilder, "outputBaseDir", config.outputBaseDir)
    config.jvmTarget?.let { setProperty(configBuilder, "jvmTarget", it) }
    config.languageVersion?.let { setProperty(configBuilder, "languageVersion", it) }
    config.apiVersion?.let { setProperty(configBuilder, "apiVersion", it) }
    config.jdkHome?.let { setProperty(configBuilder, "jdkHome", it) }
    setProperty(configBuilder, "mapAnnotationArgumentsInJava", true)

    val kspConfig = buildMethod.invoke(configBuilder)

    // Create logger and KSP instance
    val logger = loggerConstructor.newInstance(logLevel)
    val ksp = kspConstructor.newInstance(kspConfig, processors, logger)

    // Execute and get exit code
    val exitCode = executeMethod.invoke(ksp)
    val codeField = exitCode.javaClass.getDeclaredField("code")
    codeField.isAccessible = true
    return codeField.getInt(exitCode)
  }

  private fun setProperty(
    builder: Any,
    name: String,
    value: Any,
  ) {
    val setterName = "set${name.replaceFirstChar { c -> c.uppercase() }}"
    val setter =
      configSetters[setterName]
        ?: throw NoSuchMethodException("No setter $setterName found in KSPJvmConfig.Builder")
    setter.invoke(builder, value)
  }

  companion object {
    // Cache invokers by classloader identity to avoid recreating them
    private val invokerCache = mutableMapOf<ClassLoader, Ksp2Invoker>()

    /**
     * Get or create a Ksp2Invoker for the given classloader.
     * Invokers are cached to avoid repeated reflection setup.
     */
    @Synchronized
    fun forClassLoader(classLoader: ClassLoader): Ksp2Invoker =
      invokerCache.getOrPut(classLoader) { Ksp2Invoker(classLoader) }

    /**
     * Clear the invoker cache. Useful for testing or when classloaders change.
     */
    @Synchronized
    fun clearCache() {
      invokerCache.clear()
    }
  }
}
