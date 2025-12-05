/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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

package io.bazel.kotlin.builder.cmd

import io.bazel.worker.Status
import io.bazel.worker.Worker
import kotlin.system.exitProcess

/**
 * Entry point for KSP2 (Kotlin Symbol Processing) worker.
 * This enables KSP2 to run as a Bazel persistent worker, improving build performance
 * by reusing the JVM process across multiple KSP2 invocations.
 */
object Ksp2 {
  private val executor = Ksp2Executor()

  @JvmStatic
  fun main(args: Array<String>) {
    Worker
      .from(args.toList()) {
        start { ctx, arguments ->
          if (executor.execute(ctx, arguments.toList()) == 0) {
            Status.SUCCESS
          } else {
            Status.ERROR
          }
        }
      }.run(::exitProcess)
  }
}
