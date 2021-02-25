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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets.UTF_8
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
  private val captureIO: () -> IO
) : Worker {

  constructor() : this(Dispatchers.IO, IO.Companion::capture)

  /**
   * ThreadAwareDispatchers provides an ability to separate thread blocking operations from coroutines..
   *
   * Coroutines interleave actions over a pool of threads. When an action blocks it stands a chance
   * of producing a deadlock. We sidestep this by providing a separate dispatcher to contain
   * blocking operations, like reading from a stream. Inelegant, and a bit of a sledgehammer, but
   * safe for the moment.
   */
  private class BlockableDispatcher(
    private val unblockedContext: CoroutineContext,
    private val blockingContext: ExecutorCoroutineDispatcher
  ) {
    companion object {
      fun <T> runIn(
        owningContext: CoroutineContext,
        exec: suspend BlockableDispatcher.() -> T
      ) =
        Executors.newCachedThreadPool().asCoroutineDispatcher().use { dispatcher ->
          runBlocking(owningContext) { BlockableDispatcher(owningContext, dispatcher).exec() }
        }
    }

    fun <T> blockable(action: () -> T): T {
      return runBlocking(blockingContext) {
        return@runBlocking action()
      }
    }
  }

  @ExperimentalCoroutinesApi
  override fun start(execute: Work) = WorkerContext.run {
    captureIO().use { io ->
      BlockableDispatcher.runIn(coroutineContext) {
        blockable {
          generateSequence { WorkRequest.parseDelimitedFrom(io.input) }
        }.asFlow()
          .flowOn(coroutineContext)
          .map { request ->
            info { "received req: ${request.requestId}" }
            doTask("request ${request.requestId}") { ctx ->
              request.argumentsList.run {
                execute(ctx, toList())
              }
            }.let { result ->
              this@run.info { "task result ${result.status}" }
              WorkerProtocol.WorkResponse.newBuilder().apply {
                output =
                  listOf(
                    result.log.out.toString(),
                    io.captured.toByteArray().toString(UTF_8)
                  ).filter { it.isNotBlank() }.joinToString("\n")
                exitCode = result.status.exit
                requestId = request.requestId
              }.build()
            }
          }
          .collect { response ->
            blockable {
              info {
                response.toString()
              }
              response.writeDelimitedTo(io.output)
              io.output.flush()
            }
          }
      }
      io.output.close()
      info { "stopped worker" }
    }
    return@run 0
  }
}
