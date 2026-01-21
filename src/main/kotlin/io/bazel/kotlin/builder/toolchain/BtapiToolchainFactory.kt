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
package io.bazel.kotlin.builder.toolchain

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.net.URLClassLoader

/**
 * Factory for creating BTAPI KotlinToolchains with proper classloader setup.
 *
 * Creates a classloader with:
 * - build-tools-impl.jar (BTAPI implementation)
 * - kotlin-compiler-embeddable.jar (Kotlin compiler - has shaded stdlib)
 * - kotlin-stdlib.jar (non-shaded stdlib for build-tools-impl runtime)
 * - Plugin JARs (compiler plugins)
 */
@OptIn(ExperimentalBuildToolsApi::class)
class BtapiToolchainFactory(
  private val buildToolsImplJar: File,
  private val kotlinCompilerEmbeddableJar: File,
  private val kotlinStdlibJar: File,
  private val kotlinReflectJar: File,
  private val kotlinCoroutinesJar: File,
  private val annotationsJar: File,
  private val pluginJars: List<File>,
) {
  /**
   * Creates a KotlinToolchains instance with the proper classloader setup.
   */
  fun createToolchains(): KotlinToolchains {
    val classpath = mutableListOf<File>()
    classpath.add(buildToolsImplJar)
    classpath.add(kotlinCompilerEmbeddableJar)
    classpath.add(kotlinStdlibJar)
    classpath.add(kotlinReflectJar)
    classpath.add(kotlinCoroutinesJar)
    classpath.add(annotationsJar)
    classpath.addAll(pluginJars)

    val urls = classpath.map { it.toURI().toURL() }.toTypedArray()

    // SharedApiClassesClassLoader ensures API classes (from build-tools-api)
    // are shared between the caller and the loaded implementation
    val classLoader = URLClassLoader(urls, SharedApiClassesClassLoader())

    return KotlinToolchains.loadImplementation(classLoader)
  }
}
