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
import com.google.devtools.build.lib.worker.WorkerProtocol
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import org.junit.jupiter.api.Test;

class WorkerEnvironmentTest {

  @Test
  fun sendOne() {
    val give = WorkRequest.newBuilder().setRequestId(1).addArguments("foo").build()
    val got = WorkerEnvironment.inProcess {
      task { stdIn, stdOut ->
        generateSequence {
          WorkRequest.parseDelimitedFrom(stdIn).also { println("got $it") }
        }.forEach { req ->
          WorkerProtocol.WorkResponse.newBuilder().setRequestId(req.requestId).build()
            .also { println("sent $it") }
            .writeDelimitedTo(stdOut)
        }
      }

      writeStdIn(give)
      closeStdIn()

      return@inProcess readStdOut()
    }

    assertThat(got)
      .isEqualTo(WorkerProtocol.WorkResponse.newBuilder().setRequestId(1).build())
  }

  @Test
  fun send() {
    val give = (1..5).map { WorkRequest.newBuilder().setRequestId(it).addArguments("foo").build() }
    val got = WorkerEnvironment.inProcess {
      task { stdIn, stdOut ->
        generateSequence {
          WorkRequest.parseDelimitedFrom(stdIn)
        }.forEach { req ->
          WorkerProtocol.WorkResponse.newBuilder().setRequestId(req.requestId).build()
            .writeDelimitedTo(stdOut)
        }
      }

      give.forEach { writeStdIn(it) }
      closeStdIn()

      return@inProcess generateSequence { readStdOut() }.toSet()
    }

    assertThat(got).containsExactlyElementsIn(
      (1..5).map { id ->
        WorkerProtocol.WorkResponse.newBuilder().setRequestId(id).build()
      }
    )
  }
}
