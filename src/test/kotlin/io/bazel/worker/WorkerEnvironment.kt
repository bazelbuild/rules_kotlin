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

package io.bazel.worker

import com.google.devtools.build.lib.worker.WorkerProtocol
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.Closeable
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** WorkerEnvironment encapsulates the communication between the test and the worker. */
object WorkerEnvironment {

  /** inProcess communication environment for sending and receiving WorkerProtocol messages. */
  fun <T> inProcess(work: Broker.() -> T): T {
    val executor = Executors.newCachedThreadPool()
    return try {
      PipedStream().use { stdOut ->
        PipedStream().use { stdIn ->
          val responses = ConcurrentLinkedQueue<WorkResponse>()
          val reader = executor.submit {
            Thread.currentThread().name = "readStdOut"
            while (true) {
              WorkResponse.parseDelimitedFrom(stdOut.input)
                ?.let(responses::add)
                ?: break
            }
          }
          Broker(stdIn, stdOut, executor, responses).run(work).also {
            reader.get() // wait until the reader finishes
          }
        }
      }
    } finally {
      executor.shutdownNow().forEach { r ->
        println("${Thread.currentThread().name}:running task on close: $r")
      }
    }
  }

  class Broker internal constructor(
    private val stdIn: PipedStream,
    private val stdOut: PipedStream,
    private val executor: ExecutorService,
    private val responses: Queue<WorkResponse>
  ) {
    val tasks = mutableListOf<Future<*>>()

    val coroutineContext by lazy { executor.asCoroutineDispatcher() }

    fun writeStdIn(request: WorkerProtocol.WorkRequest) {
      request.writeDelimitedTo(stdIn.output)
      stdIn.output.flush()
    }

    fun closeStdIn() {
      try {
        executor.submit {
          stdIn.output.close()
          // wait for all tasks to finish.
          tasks.forEach { it.get() }
        }.get(30, TimeUnit.SECONDS)
      } catch (t: TimeoutException) {
        Thread.getAllStackTraces().forEach { (t, st) ->
          println("${t.name} (${t.isAlive}:\n ${st.asIterable().joinToString { "\n\t$it" }}")
        }
      }
    }

    fun waitForStdOut() {
      while (stdOut.input.available() != 0) {
        Thread.sleep(1) // don't starve other processes.
      }
    }

    fun readStdOut(): WorkResponse? = responses.poll()

    fun task(execute: (stdIn: InputStream, stdOut: PrintStream) -> Unit) {
      tasks.add(
        executor.submit {
          execute(stdIn.input, PrintStream(stdOut.output))
          stdOut.output.close()
        }
      )
    }
  }

  internal class PipedStream : Closeable {
    val output = PipedOutputStream()
    val input = PipedInputStream(output)

    override fun toString(): String {
      return "PipedStream($output, $input)"
    }

    override fun close() {
      input.close()
    }
  }
}
