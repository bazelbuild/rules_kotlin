/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder.toolchain

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

/**
 * Caches BTAPI toolchains keyed by runtime specification.
 *
 * A worker process may serve requests from multiple Kotlin toolchains, so toolchains
 * must be selected and cached per request rather than fixed at worker startup.
 */
@OptIn(ExperimentalBuildToolsApi::class)
class BtapiToolchainsCache {
  private val toolchainsByRuntime = ConcurrentHashMap<BtapiRuntimeSpec, KotlinToolchains>()

  fun get(runtime: BtapiRuntimeSpec): KotlinToolchains =
    toolchainsByRuntime.computeIfAbsent(runtime, ::loadToolchains)

  private fun loadToolchains(runtime: BtapiRuntimeSpec): KotlinToolchains {
    validateFilesExist(runtime)

    val compilerVersion =
      readNormalizedVersion(runtime.kotlinCompilerEmbeddableJar, "kotlin-compiler-embeddable")
    val buildToolsVersion =
      readNormalizedVersion(runtime.buildToolsImplJar, "kotlin-build-tools-impl")
    require(buildToolsVersion == compilerVersion) {
      "BTAPI runtime mismatch: kotlin-build-tools-impl version '$buildToolsVersion' " +
        "must match kotlin-compiler-embeddable version '$compilerVersion'."
    }

    val daemonVersion = readNormalizedVersion(runtime.kotlinDaemonClientJar, "kotlin-daemon-client")
    require(daemonVersion == compilerVersion) {
      "BTAPI runtime mismatch: kotlin-daemon-client version '$daemonVersion' " +
        "must match kotlin-compiler-embeddable version '$compilerVersion'."
    }

    val urls = runtime.classpath.map { it.toUri().toURL() }.toTypedArray()
    val classLoader = URLClassLoader(urls, SharedApiClassesClassLoader())
    val toolchains = KotlinToolchains.loadImplementation(classLoader)
    val btapiCompilerVersion = normalizeVersion(toolchains.getCompilerVersion())
    require(btapiCompilerVersion == compilerVersion) {
      "BTAPI implementation/compiler mismatch: KotlinToolchains compiler version " +
        "'$btapiCompilerVersion' must match kotlin-compiler-embeddable version '$compilerVersion'."
    }

    return toolchains
  }

  private fun validateFilesExist(runtime: BtapiRuntimeSpec) {
    runtime.classpath.forEach { file ->
      require(Files.isRegularFile(file)) {
        "BTAPI runtime artifact does not exist or is not a file: $file"
      }
    }
  }

  private fun readNormalizedVersion(
    jar: Path,
    artifactName: String,
  ): String {
    val rawVersion =
      JarFile(jar.toFile()).use { jarFile ->
        jarFile.manifest?.mainAttributes?.getValue(IMPLEMENTATION_VERSION)
      }
    require(!rawVersion.isNullOrBlank()) {
      "Missing '$IMPLEMENTATION_VERSION' in $artifactName manifest: $jar"
    }
    return normalizeVersion(rawVersion)
  }

  private fun normalizeVersion(version: String): String =
    version.substringBefore(RELEASE_SUFFIX).trim()

  companion object {
    private const val IMPLEMENTATION_VERSION = "Implementation-Version"
    private const val RELEASE_SUFFIX = "-release-"
  }
}
