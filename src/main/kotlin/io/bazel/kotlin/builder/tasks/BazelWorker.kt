/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder.tasks

import com.google.devtools.build.lib.worker.WorkerProtocol
import io.bazel.kotlin.builder.utils.WorkingDirectoryContext
import io.bazel.kotlin.builder.utils.wasInterrupted
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Interface for command line programs.
 *
 * This is the same thing as a main function, except not static.
 */
interface CommandLineProgram {
  /**
   * Runs blocking program start to finish.
   *
   * This function might be called multiple times throughout the life of this object. Output
   * must be sent to [System.out] and [System.err].
   *
   * @param args command line arguments
   * @return program exit code, i.e. 0 for success, non-zero for failure
   */
  fun apply(workingDir: Path, args: List<String>): Int
}

/**
 * Bazel worker runner.
 *
 * This class adapts a traditional command line program so it can be spawned by Bazel as a
 * persistent worker process that handles multiple invocations per JVM. It will also be backwards
 * compatible with being run as a normal single-invocation command.
 *
 * @param <T> delegate program type
 */
class BazelWorker(
  private val delegate: CommandLineProgram,
  private val output: PrintStream,
  private val mnemonic: String
) {
  companion object {
    const val OK = 0
    const val INTERRUPTED_STATUS = 1
    const val ERROR_STATUS = 2
  }

  fun apply(args: List<String>): Int {
    return if (args.contains("--persistent_worker"))
      runAsPersistentWorker()
    else WorkingDirectoryContext.newContext().use { ctx ->
      delegate.apply(ctx.dir, loadArguments(args, false))
    }
  }

  private fun runAsPersistentWorker(): Int {
    val realStdIn = System.`in`
    val realStdOut = System.out
    val realStdErr = System.err
    try {
      ByteArrayInputStream(ByteArray(0)).use { emptyIn ->
        ByteArrayOutputStream().use { buffer ->
          PrintStream(buffer).use { ps ->
            System.setIn(emptyIn)
            System.setOut(ps)
            System.setErr(ps)
            val invocationWorker = InvocationWorker(delegate, buffer)
            while (true) {
              val status =
                  WorkerProtocol.WorkRequest.parseDelimitedFrom(realStdIn)?.let { request ->
                    invocationWorker.invoke(loadArguments(request.argumentsList, true))
                  }?.also { (status, log) ->
                    with(WorkerProtocol.WorkResponse.newBuilder()) {
                      exitCode = status
                      output = log
                      build().writeDelimitedTo(realStdOut)
                    }
                  }?.let { (status, _) -> status } ?: OK

              if (status != OK) {
                return status
              }
              System.gc()
            }
          }
        }
      }
    } finally {
      System.setIn(realStdIn)
      System.setOut(realStdOut)
      System.setErr(realStdErr)
    }
    return OK
  }

  private fun loadArguments(args: List<String>, isWorker: Boolean): List<String> {
    if (args.isNotEmpty()) {
      val lastArg = args[args.size - 1]

      if (lastArg.startsWith("@")) {
        val pathElement = lastArg.substring(1)
        val flagFile = Paths.get(pathElement)
        if (isWorker && lastArg.startsWith("@@") || Files.exists(flagFile)) {
          if (!isWorker && mnemonic.isNotEmpty()) {
            output.printf(
                "HINT: %s will compile faster if you run: " + "echo \"build --strategy=%s=worker\" >>~/.bazelrc\n",
                mnemonic, mnemonic
            )
          }
          try {
            return Files.readAllLines(flagFile, UTF_8)
          } catch (e: IOException) {
            throw RuntimeException(e)
          }
        }
      }
    }
    return args
  }
}

class InvocationWorker(
  private val delegate: CommandLineProgram,
  private val buffer: ByteArrayOutputStream
) {

  fun invoke(args: List<String>): Pair<Int, String> = WorkingDirectoryContext.newContext()
      .use { wdCtx ->
        return try {
          delegate.apply(wdCtx.dir, args)
        } catch (e: RuntimeException) {
          if (e.wasInterrupted()) BazelWorker.INTERRUPTED_STATUS
          else BazelWorker.ERROR_STATUS.also {
            System.err.println(
                "ERROR: Worker threw uncaught exception with args: ${args}"
            )
            e.printStackTrace(System.err)
          }
        } to buffer.toString()
      }
}
