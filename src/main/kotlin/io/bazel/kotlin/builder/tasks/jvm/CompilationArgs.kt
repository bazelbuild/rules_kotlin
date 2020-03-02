/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
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

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * CompilationArgs collects the arguments for executing the Kotlin compiler.
 */
internal class CompilationArgs(
  val args: MutableList<String> = mutableListOf(),
  private val dfs: FileSystem = FileSystems.getDefault()
) {

  fun givenNotEmpty(value: String, map: (String) -> Collection<String>): CompilationArgs {
    if (value.isNotEmpty()) {
      return values(map(value))
    }
    return this
  }

  fun absolutePaths(
    paths: Collection<String>,
    toArgs: (Sequence<Path>) -> String
  ): CompilationArgs {
    return value(toArgs(paths.asSequence().map { dfs.getPath(it) }.map(Path::toAbsolutePath)))
  }

  fun value(value: String): CompilationArgs {
    args.add(value)
    return this
  }

  fun flag(value: String): CompilationArgs {
    args.add(value)
    return this
  }

  fun flag(flag: String, value: String): CompilationArgs {
    args.add(flag)
    args.add(value)
    return this
  }

  fun values(values: Collection<String>): CompilationArgs {
    args.addAll(values)
    return this
  }

  fun list(): List<String> = args.toList()
}
