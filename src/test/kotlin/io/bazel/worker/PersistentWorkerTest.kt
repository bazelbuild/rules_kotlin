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
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

@ExperimentalCoroutinesApi
class PersistentWorkerTest {

  @Test
  fun multiplexOk() {
    val requests = listOf(
      WorkRequest.newBuilder().addAllArguments(listOf("--mammal", "bunny")).setRequestId(1)
        .build(),
      WorkRequest.newBuilder().addAllArguments(listOf("--mammal", "squirrel")).setRequestId(2)
        .build()
    )

    val expectedResponses = mapOf(
      1 to WorkResponse
        .newBuilder()
        .setRequestId(1)
        .setOutput("sidhe disciplined\n\nSqueek!")
        .setExitCode(1),
      2 to WorkResponse.newBuilder().setRequestId(2).setOutput("sidhe commended").setExitCode(0)
    )

    val captured = ByteArrayOutputStream()

    val actualResponses = WorkerEnvironment.inProcess {
      task { stdIn, stdOut ->
        PersistentWorker(coroutineContext) {
          IO(
            stdIn,
            stdOut,
            captured
          )
        }.start { ctx, args ->
          when (args.toList()) {
            listOf("--mammal", "bunny") -> {
              ctx.info { "sidhe disciplined" }
              captured.write("Squeek!".toByteArray(UTF_8))
              return@start Status.ERROR
            }
            listOf("--mammal", "squirrel") -> {
              ctx.info { "sidhe commended" }
              return@start Status.SUCCESS
            }
            else -> throw IllegalArgumentException("unexpected forest: $args")
          }
        }
      }
      requests.forEach { writeStdIn(it) }
      closeStdIn()
      return@inProcess generateSequence { readStdOut() }
    }.associateBy { wr -> wr.requestId }

    assertThat(actualResponses.keys).isEqualTo(expectedResponses.keys)

    expectedResponses.forEach { (resId, res) ->
      assertThat(actualResponses[resId]?.output).contains(res.output)
      assertThat(actualResponses[resId]?.exitCode).isEqualTo(res.exitCode)
    }
  }

  @Test
  fun error() {
    val captured = ByteArrayOutputStream()
    val actualResponses = WorkerEnvironment.inProcess {
      task { stdIn, stdOut ->
        PersistentWorker(coroutineContext) {
          IO(
            stdIn,
            stdOut,
            captured
          )
        }.start { _, _ ->
          throw IllegalArgumentException("missing forest fairy")
        }
      }
      writeStdIn(
        WorkRequest.newBuilder()
          .addAllArguments(listOf("--mammal", "bunny"))
          .setRequestId(1)
          .build()
      )
      closeStdIn()
      return@inProcess readStdOut()
    }

    assertThat(actualResponses?.requestId).isEqualTo(1)
    assertThat(actualResponses?.output)
      .contains("java.lang.IllegalArgumentException: missing forest fairy")
  }
}
