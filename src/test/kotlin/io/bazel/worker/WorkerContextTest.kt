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

import com.google.common.truth.Truth.assertThat
import io.bazel.worker.ContextLog.Granularity.DEBUG
import io.bazel.worker.Status.SUCCESS
import org.junit.Test

class WorkerContextTest {
  @Test
  fun logging() {
    var outerLog: List<String> = emptyList()
    val result = WorkerContext.run(
      named = "logging",
      verbose = DEBUG,
      report = { log -> outerLog = log.out.split("\n") }
    ) {
      info { "outer context" }
      return@run doTask("work") { ctx ->
        ctx.info { "inner context" }
        SUCCESS
      }
    }
    assertThat(result.status).isEqualTo(SUCCESS)
    assertThat(result.log.toString()).contains("logging work\nINFO: inner context")
    assertThat(outerLog).containsAtLeast(
      "INFO: outer context",
      "INFO: inner context"
    )
  }
}
