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
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * PersistentWorker satisfies Bazel persistent worker protocol for executing work.
 *
 * Supports multiplex (https://docs.bazel.build/versions/master/multiplex-worker.html) provided
 * the work is thread/coroutine safe.
 *
 * @param coroutineContext for non-threaded operations.
 * @param captureIO to avoid writing stdout and stderr while executing.
 */
class PersistentWorker(
  private val coroutineContext: CoroutineContext,
  private val captureIO: () -> IO,
  private val cpuTimeBasedGcScheduler: CpuTimeBasedGcScheduler,
) : Worker {

  constructor() : this(Dispatchers.IO, IO.Companion::capture, CpuTimeBasedGcScheduler(Duration.ofSeconds(10)))

  @ExperimentalCoroutinesApi
  override fun start(execute: Work) = WorkerContext.run {
    //Use channel to serialize writing output
    val writeChannel = Channel<WorkerProtocol.WorkResponse>(UNLIMITED)
    captureIO().use { io ->
      runBlocking {
        //Parent coroutine to track all of children and close channel on completion
        launch(Dispatchers.Default) {
            generateSequence { WorkRequest.parseDelimitedFrom(io.input) }
              .forEach { request ->
                launch {
                  compileWork(request, io, writeChannel, execute)
                  //Be a friendly worker by performing a GC between compilation requests
                  cpuTimeBasedGcScheduler.maybePerformGc()
                }
              }
        }.invokeOnCompletion { writeChannel.close() }

        writeChannel.consumeAsFlow()
          .collect { response -> writeOutput(response, io.output) }
      }

      io.output.close()
      info { "stopped worker" }
    }
    return@run 0
  }

  private suspend fun WorkerContext.compileWork(
    request: WorkRequest,
    io: IO,
    chan: Channel<WorkerProtocol.WorkResponse>,
    execute: Work
  ) = withContext(Dispatchers.Default) {
      val result = doTask("request ${request.requestId}") { ctx ->
        request.argumentsList.run {
          execute(ctx, toList())
        }
      }
      info { "task result ${result.status}" }
      val response = WorkerProtocol.WorkResponse.newBuilder().apply {
        output = listOf(
          result.log.out.toString(),
          io.captured.toByteArray().toString(UTF_8)
        ).filter { it.isNotBlank() }.joinToString("\n")
        exitCode = result.status.exit
        requestId = request.requestId
      }.build()
      info {
        response.toString()
      }
      chan.send(response)
  }

  private suspend fun writeOutput(response: WorkerProtocol.WorkResponse, output: PrintStream) =
    withContext(Dispatchers.IO) {
      response.writeDelimitedTo(output)
      output.flush()
    }

}
