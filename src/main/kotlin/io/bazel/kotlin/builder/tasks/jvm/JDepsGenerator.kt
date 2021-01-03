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
import io.bazel.kotlin.builder.toolchain.CompilationException
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.joinedClasspath
import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.rootCause
import io.bazel.kotlin.model.JvmCompilationTask
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JDepsGenerator @Inject constructor(
  toolchain: KotlinToolchain,
  private val invoker: KotlinToolchain.JDepsInvoker
) {
  private val isKotlinImplicit = JdepsParser.pathSuffixMatchingPredicate(
    toolchain.kotlinHome.resolveVerified("lib").toPath(),
    *toolchain.kotlinStandardLibraries.toTypedArray()
  )

  fun generateJDeps(command: JvmCompilationTask) {
    val jdepsContent =
      if (command.inputs.classpathList.isEmpty()) {
        Deps.Dependencies.newBuilder().let {
          it.ruleLabel = command.info.label
          it.build()
        }
      } else {
        ByteArrayOutputStream().use { out ->
          PrintWriter(out).use { writer ->
            val joinedClasspath = command.inputs.joinedClasspath
            val version = System.getProperty("java.version").majorJavaVersion()
            val multiRelease =
              if (version < 9) arrayOf() else arrayOf("--multi-release", "base")
            val javaClassDir = command.directories.javaClasses
            val args = multiRelease + arrayOf("-R", "-summary", "-cp", joinedClasspath, javaClassDir)
            val res = invoker.run(args, writer)
            out.toByteArray().inputStream().bufferedReader().readLines().let {
              if (res != 0) {
                throw CompilationStatusException("could not run jdeps tool", res, it)
              } else try {
                JdepsParser.parse(
                  command.info.label,
                  javaClassDir,
                  command.inputs.directDependenciesList,
                  it,
                  isKotlinImplicit
                )
              } catch (e: Exception) {
                throw CompilationException("error reading or parsing jdeps file", e.rootCause)
              }
            }
          }
        }
      }
    Paths.get(command.outputs.javaJdeps).also {
      Files.deleteIfExists(it)
      FileOutputStream(Files.createFile(it).toFile()).use(jdepsContent::writeTo)
    }
  }
}

/**
 * Extract the normalized major version from the java.version property.
 *
 * This is more complex than simply taking the first term because later versions of the JVM changed
 * from reporting the version in the `1.x.y_z` format (e.g. `1.8.0_202`) in favor of `x.y.z` (e.g.
 * `11.0.1`). As a result, this function checks for a major version of "1" and if it's so, use the
 * minor version as the major one. Otherwise, it uses the first term as the major version.
 */
private fun String.majorJavaVersion(): Int {
  val (major, minor) = this.trim().split('.')
  val parsedMajor = Integer.parseInt(major)
  return if (parsedMajor == 1) Integer.parseInt(minor) else parsedMajor
}
