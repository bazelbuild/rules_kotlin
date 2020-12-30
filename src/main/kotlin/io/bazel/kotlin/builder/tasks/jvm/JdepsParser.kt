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
package io.bazel.kotlin.builder.tasks.jvm

import com.google.devtools.build.lib.view.proto.Deps
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.function.Predicate

internal class JdepsParser private constructor(
  private val isImplicit: Predicate<String>
) {
  private val depMap = HashMap<String, Deps.Dependency.Builder>()
  private val moduleDeps = HashMap<String, MutableList<String>>()
  private val arrowRegex = " -> ".toRegex()

  private fun consumeJarLine(classJarPath: String, kind: Deps.Dependency.Kind) {
    // ignore absolute files, -- jdk jar paths etc.
    // only process jar files
    if (classJarPath.endsWith(".jar")) {
      val entry = depMap.computeIfAbsent(classJarPath) {
        val depBuilder = Deps.Dependency.newBuilder()
        depBuilder.path = classJarPath
        depBuilder.kind = kind

        if (isImplicit.test(classJarPath)) {
          depBuilder.kind = Deps.Dependency.Kind.IMPLICIT
        }
        depBuilder
      }

      // don't flip an implicit dep.
      if (entry.kind != Deps.Dependency.Kind.IMPLICIT) {
        entry.kind = kind
      }
    }
  }

  private fun addModuleDependency(module: String, jarFile: String) {
    val entry = moduleDeps.computeIfAbsent(module) {
      mutableListOf<String>()
    }
    entry.add(jarFile)
  }

  private fun processLine(line: String) {
    val parts = line.split(arrowRegex).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (parts.size == 2 && parts[1].endsWith(".jar")) {
      addModuleDependency(parts[0], parts[1]);
    }
  }

  private fun markFromEntry(entry: String, kind: Deps.Dependency.Kind) {
    moduleDeps[entry]?.forEach { jarPath ->
      val dependency = depMap[jarPath]
      if (dependency != null) {
        if (dependency.kind == Deps.Dependency.Kind.UNUSED) {
          dependency.kind = kind
          val jarFile = Paths.get(jarPath).getFileName().toString()
          markFromEntry(jarFile, Deps.Dependency.Kind.IMPLICIT)
        }
      }
    }
  }

  companion object {
    fun pathSuffixMatchingPredicate(directory: Path, vararg jars: String): Predicate<String> {
      val suffixes = jars.map { directory.resolve(it).toString() }
      return Predicate { jar ->
        for (implicitJarsEnding in suffixes) {
          if (jar.endsWith(implicitJarsEnding)) {
            return@Predicate true
          }
        }
        false
      }
    }

    fun parse(
      label: String,
      jarFile: String,
      classPath: MutableList<String>,
      lines: List<String>,
      isImplicit: Predicate<String>
    ): Deps.Dependencies {
      val filename = Paths.get(jarFile).fileName.toString()
      val jdepsParser = JdepsParser(isImplicit)

      classPath.forEach { x -> jdepsParser.consumeJarLine(x, Deps.Dependency.Kind.UNUSED) }
      lines.forEach { jdepsParser.processLine(it) }
      jdepsParser.markFromEntry(filename, Deps.Dependency.Kind.EXPLICIT)

      val rootBuilder = Deps.Dependencies.newBuilder()
      rootBuilder.success = false
      rootBuilder.ruleLabel = label
      jdepsParser.depMap.values.forEach { b -> rootBuilder.addDependency(b.build()) }
      rootBuilder.success = true
      return rootBuilder.build()
    }
  }
}
