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

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse
import src.main.kotlin.io.bazel.worker.GcScheduler
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * PersistentWorker satisfies Bazel persistent worker protocol for executing work.
 *
 * Supports multiplex (https://docs.bazel.build/versions/master/multiplex-worker.html) provided
 * the work is thread/coroutine safe.
 *
 * @param executor thread pool for executing tasks.
 * @param captureIO to avoid writing stdout and stderr while executing.
 * @param cpuTimeBasedGcScheduler to trigger gc cleanup.
 */
class PersistentWorker(
  private val captureIO: () -> IO,
  private val executor: ExecutorService,
  private val cpuTimeBasedGcScheduler: GcScheduler
) : Worker {

  constructor(
    executor: ExecutorService,
    captureIO: () -> IO,
  ) : this(
    captureIO,
    executor,
    GcScheduler {}
  )

  constructor() : this(
    IO.Companion::capture,
    Executors.newCachedThreadPool(),
    CpuTimeBasedGcScheduler(Duration.ofSeconds(10))
  )

  override fun start(execute: Work) = WorkerContext.run {
    captureIO().use { io ->
      val running = AtomicLong(0)
      val completion = ExecutorCompletionService<WorkResponse>(executor)
      val producer = executor.submit {
        io.input.readRequestAnd { request ->
          running.incrementAndGet()
          completion.submit {
            doTask(
              name = "request ${request.requestId}",
              task = request.workTo(execute)
            ).asResponseTo(request.requestId, io)
          }
        }
      }
      val consumer = executor.submit {
        while (!producer.isDone || running.get() > 0) {
          // poll time is how long before checking producer liveliness. Too long, worker hangs
          // when being shutdown -- too short, and it starves the process.
          completion.poll(1, TimeUnit.SECONDS)?.run {
            running.decrementAndGet()
            get().writeDelimitedTo(io.output)
            io.output.flush()
          }
          cpuTimeBasedGcScheduler.maybePerformGc()
        }
      }
      producer.get()
      consumer.get()
      io.output.close()
    }
    return@run 0
  }

  private fun WorkRequest.workTo(execute: Work): (sub: WorkerContext.TaskContext) -> Status {
    return { ctx -> execute(ctx, argumentsList.toList()) }
  }

  private fun InputStream.readRequestAnd(action: (WorkRequest) -> Unit) {
    while (true) {
      WorkRequest.parseDelimitedFrom(this)
        ?.run(action)
        ?: return
    }
  }

  private fun TaskResult.asResponseTo(id: Int, io: IO): WorkResponse {
    return WorkResponse.newBuilder()
      .apply {
        val cap = io.readCapturedAsUtf8String()
        // append whatever falls through standard out.
        output = listOf(
          log.out.toString(),
          cap
        ).joinToString("\n").trim()
        exitCode = status.exit
        requestId = id
      }
      .build()
  }
}
