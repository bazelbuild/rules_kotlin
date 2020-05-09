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

package io.bazel.kotlin.builder.tasks

import com.google.common.truth.Truth.assertThat
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path

class BazelWorkerTest {

  val passingProgram: CommandLineProgram = object : CommandLineProgram {
    override fun apply(workingDir: Path, args: List<String>): Int {
      return 0
    }
  }

  val failingProgram: CommandLineProgram = object : CommandLineProgram {
    override fun apply(workingDir: Path, args: List<String>): Int {
      return 5
    }
  }

  @Test
  fun invocationWorkerPass() {
    assertThat(InvocationWorker(WorkerIO.noop(), passingProgram).run(listOf())).isEqualTo(0)
  }

  @Test
  fun invocationWorkerFail() {
    assertThat(InvocationWorker(WorkerIO.noop(), failingProgram).run(listOf())).isEqualTo(5)
  }

  @get:Rule
  val timeout = CoroutinesTimeout.seconds(15)

  @Test
  fun persistentWorker() {
    val workerInput = WorkerChannel("in")
    val workerOutput = WorkerChannel("out")
    runBlockingTest {
      val program = object : CommandLineProgram {
        override fun apply(workingDir: Path, args: List<String>): Int {
          return when (args.last()) {
            "ok" -> 0
            "fail" -> 255
            "interrupt" -> throw InterruptedException("interrupted")
            "error" -> throw RuntimeException("unexpected")
            else -> throw IllegalArgumentException("unknown $args")
          }
        }
      }

      val execution = ByteArrayOutputStream()

      val done = GlobalScope.async(CoroutineName("worker")) {
        WorkerIO(workerInput.input,
                 PrintStream(workerOutput.output),
                 execution) {}.use { io ->
          PersistentWorker(io, program).run(listOf())
        }
      }

      // asserts scope to ensure all asserts are run.
      // messy, can be cleaned up -- since kotlin channels love to block and can easily starve a
      // dispatcher, it's necessary to read each channel in a different coroutine.
      // The coroutineScope prevents the assertions from happening outside of the expected
      // asynchronicity.
      coroutineScope {
        launch {
          workerInput.send(request(1, "ok"))
        }
        launch {
          assertThat(workerOutput.read())
            .isEqualTo(response(1, 0))
        }
      }

      coroutineScope {
        launch {
          workerInput.send(request(2, "fail"))
        }
        launch {
          assertThat(workerOutput.read())
            .isEqualTo(response(2, 255))
        }
      }

      coroutineScope {
        launch {
          workerInput.send(request(3, "error"))
        }
        launch {
          assertThat(workerOutput.read())
            .isEqualTo(response(3, 1, "unexpected"))
        }
      }

      // an interrupt kills the worker.
      coroutineScope {
        launch {
          workerInput.send(request(4, "interrupt"))
          workerInput.close()
        }
        launch {
          assertThat(workerOutput.read())
            .isEqualTo(response(4, 1, "interrupted"))
        }
      }


      assertThat(done.await()).isEqualTo(0)
    }
  }

  private fun request(id: Int, vararg args: String) = with(WorkRequest.newBuilder()) {
    setRequestId(id)
    addAllArguments(args.asList())
  }.build()

  private fun response(id: Int, exitCode: Int, output: String = "") =
    with(WorkResponse.newBuilder()) {
      setExitCode(exitCode)
      setOutput(output)
      setRequestId(id)
    }.build()

  /** WorkerChannel encapsulates the communication between the test and the worker. */
  class WorkerChannel(
    val name: String,
    val channel: Channel<Byte> = Channel(20),
    val input: ChannelInputStream = ChannelInputStream(channel, name),
    val output: ChannelOutputStream = ChannelOutputStream(channel, name)
  ) {
    fun send(work: WorkRequest) {
      work.writeDelimitedTo(output)
    }

    fun read(): WorkResponse? {
      return WorkResponse.parseDelimitedFrom(input)
    }

    fun close() {
      channel.close()
    }
  }

  /** ChannelInputStream implements the InputStream using a channel and coroutines. */
  class ChannelInputStream(val channel: Channel<Byte>, val name: String) : InputStream() {
    override fun read(): Int {
      return runBlocking(CoroutineName("$name.read()")) {
        // since pipes block until the next event, this simulates that without starving
        // other routines.
        while (channel.isEmpty) {
          delay(5L)
        }
        // read blocking -- this better simulates the java InputStream behaviour.
        return@runBlocking channel.receive().toInt()
      }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
      return runBlocking(CoroutineName("$name.read(ByteArray,Int,Int)")) {
        // since pipes block until the next event, this simulates that without starving
        // other routines.
        while (channel.isEmpty) {
          delay(5L)
        }
        val end = Math.min(b.size, off + len - 1)
        var read = 0
        for (i in off..end) {
          val rb = channel.receive()
          b[i] = rb
          read++
        }
        return@runBlocking read
      }
    }
  }

  /** ChannelOutputStream implements the OutputStream using a channel and coroutines. */
  class ChannelOutputStream(val channel: Channel<Byte>, val name: String) : OutputStream() {
    override fun write(b: Int) {
      write(byteArrayOf(b.toByte()))
    }

    override fun write(ba: ByteArray, off: Int, len: Int) {
      runBlocking(CoroutineName("$name.write(ByteArray, Int, Int)")) {
        var sent = 0
        for (i in off..Math.min(ba.size, off + len - 1)) {
          channel.sendBlocking(ba[i])
          sent++
        }
      }
    }
  }
}

