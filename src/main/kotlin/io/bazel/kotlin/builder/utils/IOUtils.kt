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
@file:JvmName("IOUtils")

package io.bazel.kotlin.builder.utils

import java.io.BufferedReader
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private fun executeAwait(timeoutSeconds: Int, process: Process): Int {
  try {
    if (!process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)) {
      throw TimeoutException()
    }
    return process.exitValue()
  } finally {
    if (process.isAlive) {
      process.destroy()
    }
  }
}

fun executeAndAwait(timeoutSeconds: Int, directory: File? = null, args: List<String>): Int {
  val process = ProcessBuilder(*args.toTypedArray()).let { pb ->
    pb.redirectError(ProcessBuilder.Redirect.PIPE)
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
    directory?.also { pb.directory(it) }
    pb.start()
  }

  var isr: BufferedReader? = null
  var esr: BufferedReader? = null

  try {
    isr = process.inputStream.bufferedReader()
    esr = process.errorStream.bufferedReader()
    return executeAwait(timeoutSeconds, process)
  } finally {
    isr?.drainTo(System.out)
    esr?.drainTo(System.err)
  }
}

private fun BufferedReader.drainTo(pw: PrintStream) {
  lines().forEach(pw::println); close()
}

fun Path.resolveTwinVerified(extension: String): Path =
  parent.resolve(
    "${toFile().nameWithoutExtension}${if (extension.startsWith(".")) "" else "."}$extension"
  ).verified().toPath()

fun Path.resolveVerified(vararg parts: String): File =
  resolve(Paths.get(parts[0], *Arrays.copyOfRange(parts, 1, parts.size))).verified()

fun Path.resolveVerifiedToAbsoluteString(vararg parts: String): String =
  resolveVerified(*parts).absolutePath.toString()

fun Path.verified(): File =
  this.toFile().also { check(it.exists()) { "file did not exist: $this" } }

fun Path.verifiedPath(): Path =
  this.toFile().also { check(it.exists()) { "file did not exist: $this" } }.toPath()

fun ensureDirectories(vararg directories: String) {
  for (directory in directories) {
    Files.createDirectories(Paths.get(directory))
  }
}

val Throwable.rootCause: Throwable
  get() {
    var result = this
    do {
      val cause = result.cause
      if (cause != null) result = cause
    } while (cause != null && result != cause)
    return result
  }
