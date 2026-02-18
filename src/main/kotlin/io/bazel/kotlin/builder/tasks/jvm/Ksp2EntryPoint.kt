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

package io.bazel.kotlin.builder.tasks.jvm

import java.io.File

/**
 * Entry point invoked by the KSP2 worker through a dedicated classloader.
 *
 * Implementations may depend on KSP2 runtime classes; this interface stays in the worker classpath
 * so the worker can call into the implementation without reflective method lookup.
 */
interface Ksp2EntryPoint {
  fun execute(
    moduleName: String,
    sourceRoots: List<File>,
    javaSourceRoots: List<File>,
    libraries: List<File>,
    kotlinOutputDir: File,
    javaOutputDir: File,
    classOutputDir: File,
    resourceOutputDir: File,
    cachesDir: File,
    projectBaseDir: File,
    outputBaseDir: File,
    jvmTarget: String?,
    languageVersion: String?,
    apiVersion: String?,
    jdkHome: File?,
    logLevel: Int,
  ): Int
}
