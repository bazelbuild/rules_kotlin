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
import src.main.kotlin.io.bazel.worker.GcScheduler
import java.time.Duration
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport
import java.util.stream.StreamSupport.stream

/**
 * PersistentWorker satisfies Bazel persistent worker protocol for executing work.
 *
 * Supports multiplex (https://docs.bazel.build/versions/master/multiplex-worker.html) provided
 * the work is thread/coroutine safe.
 *
 * @param coroutineContext for non-threaded operations.
 * @param captureIO to avoid writing stdout and stderr while executing.
 */
class JavaPersistentWorker(
  private val captureIO: () -> IO,
  private val executor: ForkJoinPool,
  private val cpuTimeBasedGcScheduler: GcScheduler
) : Worker {

  constructor(
    executor: ForkJoinPool,
    captureIO: () -> IO,
  ) : this(
    captureIO,
    executor,
    GcScheduler {}
  )

  constructor() : this(IO.Companion::capture,
    ForkJoinPool(),
    CpuTimeBasedGcScheduler(Duration.ofSeconds(10))
  )


  private class UntilNull<T> private constructor(val next: () -> T) :
    Spliterators.AbstractSpliterator<T>(
      Long.MAX_VALUE,
      Spliterator.CONCURRENT or Spliterator.IMMUTABLE
    ) {

    companion object {
      fun <T> parallelStream(next: () -> T): Stream<T> = stream(UntilNull(next), true)
    }

    override fun tryAdvance(action: Consumer<in T>): Boolean {
      return next()
        ?.apply(action::accept)
        ?.run { true }
        ?: false
    }
  }

  override fun start(execute: Work) = WorkerContext.run {
    captureIO().use { io ->
      executor.submit {
        UntilNull
          .parallelStream { WorkRequest.parseDelimitedFrom(io.input) }
          .map { request -> handle(execute, request, io = io) }
          .forEach { response ->
            response.writeDelimitedTo(io.output)
            io.output.flush()
            cpuTimeBasedGcScheduler.maybePerformGc()
          }
      }
      io.output.close()
      info { "stopped worker" }
    }
    return@run 0
  }

  private fun WorkerContext.handle(execute: Work, request: WorkRequest, io: IO): WorkerProtocol.WorkResponse {
    val result = doTask("request ${request.requestId}") { ctx ->
      request.argumentsList.run {
        execute(ctx, toList())
      }
    }
    info { "task result ${result.status}" }
    return WorkerProtocol.WorkResponse.newBuilder().apply {
      val cap = io.readCapturedAsUtf8String()
      // append whatever falls through standard out.
      output = listOf(
        result.log.out.toString(),
        cap
      ).joinToString("\n").trim()
      exitCode = result.status.exit
      requestId = request.requestId
    }.build().also { response ->
      info {
        response.toString()
      }
    }
  }

}
