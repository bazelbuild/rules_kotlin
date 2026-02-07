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

import java.nio.file.Path

/**
 * Runtime configuration needed to load a BTAPI implementation and matching Kotlin compiler.
 */
data class BtapiRuntimeSpec(
  val buildToolsImplJar: Path,
  val kotlinCompilerEmbeddableJar: Path,
  val kotlinDaemonClientJar: Path,
  val kotlinStdlibJar: Path,
  val kotlinReflectJar: Path,
  val kotlinCoroutinesJar: Path,
  val annotationsJar: Path,
) {
  val classpath: List<Path>
    get() =
      listOf(
        buildToolsImplJar,
        kotlinDaemonClientJar,
        kotlinCompilerEmbeddableJar,
        kotlinStdlibJar,
        kotlinReflectJar,
        kotlinCoroutinesJar,
        annotationsJar,
      )

  companion object {
    fun fromJarPaths(
      buildToolsImplJar: String,
      kotlinCompilerEmbeddableJar: String,
      kotlinDaemonClientJar: String,
      kotlinStdlibJar: String,
      kotlinReflectJar: String,
      kotlinCoroutinesJar: String,
      annotationsJar: String,
    ): BtapiRuntimeSpec =
      BtapiRuntimeSpec(
        buildToolsImplJar = Path.of(buildToolsImplJar),
        kotlinCompilerEmbeddableJar = Path.of(kotlinCompilerEmbeddableJar),
        kotlinDaemonClientJar = Path.of(kotlinDaemonClientJar),
        kotlinStdlibJar = Path.of(kotlinStdlibJar),
        kotlinReflectJar = Path.of(kotlinReflectJar),
        kotlinCoroutinesJar = Path.of(kotlinCoroutinesJar),
        annotationsJar = Path.of(annotationsJar),
      )
  }
}
