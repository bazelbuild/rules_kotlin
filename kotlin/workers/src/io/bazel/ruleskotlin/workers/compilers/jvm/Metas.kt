/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package io.bazel.ruleskotlin.workers.compilers.jvm

import io.bazel.ruleskotlin.workers.CompileResult
import io.bazel.ruleskotlin.workers.Meta

import java.nio.file.Path

/**
 * Meta is a key to some compilation state,.
 */
object Metas {
    // mandatory: the package part of the label.
    val PKG = Meta<String>("package")
    // mandatory: The target part of the label.
    val TARGET = Meta<String>("target")
    // mandatory: the class staging directory.
    val CLASSES_DIRECTORY = Meta<Path>("class_directory")
    // mandatory: If this is non empty then it is a mixed mode operation.
    val JAVA_SOURCES = Meta<List<String>>("java_sources")
    // mandatory:
    val ALL_SOURCES = Meta<List<String>>("all_sources")
    // mandatory:
    val KOTLINC_RESULT = CompileResult.Meta("kotlin_compile_result")
    // optional: when not a mixed mode operation.
    val JAVAC_RESULT = CompileResult.Meta("javac_compile_result")
}