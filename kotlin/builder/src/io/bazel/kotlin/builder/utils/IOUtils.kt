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

import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun executeAndWaitOutput(timeoutSeconds: Int, vararg command: String): List<String> {
    try {
        val builder = ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = builder.start()
        val al = ArrayList<String>()
        var streamReader: CompletableFuture<Void>? = null
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { output ->
                streamReader = CompletableFuture.runAsync {
                    while (true) {
                        try {
                            val line = output.readLine() ?: break
                            al.add(line)
                        } catch (e: IOException) {
                            throw UncheckedIOException(e)
                        }

                    }
                }
                executeAwait(timeoutSeconds, process)
                return al
            }
        } finally {
            if (streamReader != null && !streamReader!!.isDone) {
                streamReader!!.cancel(true)
            }
        }
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

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

fun executeAndAwait(timeoutSeconds: Int, args: List<String>): Int {
    val process = ProcessBuilder(*args.toTypedArray()).let {
        it.redirectError(ProcessBuilder.Redirect.PIPE)
        it.redirectOutput(ProcessBuilder.Redirect.PIPE)
        it.start()
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

fun executeAndAwaitSuccess(timeoutSeconds: Int, vararg command: String) {
    val status = executeAndAwait(timeoutSeconds, command.toList())
    check(status == 0) {
        "process failed with status: $status"
    }
}

private fun BufferedReader.drainTo(pw: PrintStream) {
    lines().forEach(pw::println); close()
}

fun Path.purgeDirectory() {
    toFile().listFiles().forEach { check(it.deleteRecursively()) { "$it could not be deleted" } }
}

fun Path.resolveVerified(vararg parts: String): File = resolve(Paths.get(parts[0], *Arrays.copyOfRange(parts, 1, parts.size))).verified()

/**
 * Return a stream of paths that are known to exists relative to this location.
 */
fun Path.verifiedRelativeFiles(vararg paths: Path): List<File> = paths.map { relative -> resolve(relative).verified() }

private fun Path.verified(): File = this.toFile().also { check(it.exists()) { "file did not exist: $this" } }


val Throwable.rootCause: Throwable
    get() {
        var result = this
        do {
            val cause = result.cause
            if (cause != null) result = cause
        } while (cause != null && result != cause)
        return result
    }
