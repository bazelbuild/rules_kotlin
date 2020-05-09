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

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse
import io.bazel.kotlin.builder.utils.WorkingDirectoryContext
import io.bazel.kotlin.builder.utils.wasInterrupted
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Level.SEVERE
import java.util.logging.Logger

/**
 * Interface for command line programs.
 *
 * This is the same thing as a main function, except not static.
 */
@FunctionalInterface
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
  private val commandLineProgram: CommandLineProgram,
  private val output: PrintStream,
  private val mnemonic: String
) {

  fun apply(args: List<String>): Int {
    if (args.contains("--persistent_worker")) {
      return WorkerIO.open().use { io ->
        PersistentWorker(io, commandLineProgram).run(args)
      }
    } else {
      output.println(
        "HINT: $mnemonic will compile faster if you run: echo \"build --strategy=$mnemonic=worker\" >>~/.bazelrc"
      )
      return WorkerIO.noop().use { io ->
        InvocationWorker(io, commandLineProgram).run(args)
      }
    }
  }
}

private fun maybeExpand(args: List<String>): List<String> {
  if (args.isNotEmpty()) {
    val lastArg = args[args.size - 1]
    if (lastArg.startsWith("@")) {
      val pathElement = lastArg.substring(1)
      val flagFile = Paths.get(pathElement)
      try {
        return Files.readAllLines(flagFile, UTF_8)
      } catch (e: IOException) {
        throw RuntimeException(e)
      }
    }
  }
  return args
}

/** Defines the common worker interface. */
interface Worker {
  fun run(args: List<String>): Int
}

class WorkerIO(
  val input: InputStream,
  val output: PrintStream,
  val execution: ByteArrayOutputStream,
  private val restore: () -> Unit
) : Closeable {
  companion object {
    fun open(): WorkerIO {
      val stdErr = System.err
      val stdIn = BufferedInputStream(System.`in`)
      val stdOut = System.out
      val inputBuffer = ByteArrayInputStream(ByteArray(0))
      val execution = ByteArrayOutputStream()
      val outputBuffer = PrintStream(execution)

      // delegate the system defaults to capture execution information
      System.setErr(outputBuffer)
      System.setOut(outputBuffer)
      System.setIn(inputBuffer)

      return WorkerIO(stdIn, stdOut, execution) {
        System.setOut(stdOut)
        System.setIn(stdIn)
        System.setErr(stdErr)
      }
    }

    fun noop(): WorkerIO {
      val inputBuffer = ByteArrayInputStream(ByteArray(0))
      val execution = ByteArrayOutputStream()
      val outputBuffer = PrintStream(execution)
      return WorkerIO(inputBuffer, outputBuffer, execution) {}
    }
  }

  override fun close() {
    restore.invoke()
  }
}

/** PersistentWorker follows the Bazel worker protocol and executes a CommandLineProgram. */
class PersistentWorker(
  private val io: WorkerIO,
  private val program: CommandLineProgram
) : Worker {
  private val logger = Logger.getLogger(PersistentWorker::class.java.canonicalName)

  enum class Status {
    OK, INTERRUPTED, ERROR
  }

  override fun run(args: List<String>): Int {
    while (true) {
      val request = WorkRequest.parseDelimitedFrom(io.input) ?: continue

      val (status, exit) = WorkingDirectoryContext.newContext()
        .runCatching {
          request.argumentsList
            ?.let { maybeExpand(it) }
            .run {
              Status.OK to program.apply(dir, maybeExpand(request.argumentsList))
            }
        }
        .recover { e: Throwable ->
          io.execution.write((e.message ?: e.toString()).toByteArray(UTF_8))
          if (!e.wasInterrupted()) {
            logger.log(SEVERE,
                       "ERROR: Worker threw uncaught exception",
                       e)
            Status.ERROR to 1
          } else {
            Status.INTERRUPTED to 1
          }
        }
        .getOrThrow()

      val response = WorkResponse.newBuilder().apply {
        output = String(io.execution.toByteArray(), UTF_8)
        exitCode = exit
        requestId = request.requestId
      }.build()

      // return the response
      response.writeDelimitedTo(io.output)
      io.output.flush()

      // clear execution logs
      io.execution.reset()

      if (status == Status.INTERRUPTED) {
        break
      }
    }
    logger.info("Shutting down worker.")
    return 0
  }
}

class InvocationWorker(
  private val io: WorkerIO,
  private val program: CommandLineProgram
) : Worker {
  private val logger: Logger = Logger.getLogger(InvocationWorker::class.java.canonicalName)
  override fun run(args: List<String>): Int = WorkingDirectoryContext.newContext()
    .runCatching { program.apply(dir, maybeExpand(args)) }
    .recover { e ->
      logger.log(SEVERE,
                 "ERROR: Worker threw uncaught exception with args: ${maybeExpand(args)}",
                 e)
      return@recover 1 // return non-0 exitcode
    }
    .also {
      // print execution log
      println(String(io.execution.toByteArray(), UTF_8))
    }
    .getOrDefault(0)
}
