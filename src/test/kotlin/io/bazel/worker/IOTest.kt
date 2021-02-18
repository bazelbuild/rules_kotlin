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
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class IOTest {

  private fun ByteArrayOutputStream.written() = String(toByteArray(), StandardCharsets.UTF_8)

  private val stdErr = System.err
  private val stdIn = BufferedInputStream(System.`in`)
  private val stdOut = System.out
  private val inputBuffer = ByteArrayInputStream(ByteArray(0))
  private val captured = ByteArrayOutputStream()
  private val outputBuffer = PrintStream(captured)

  @Before
  fun captureSystem() {
    // delegate the system defaults to capture execution information
    System.setErr(outputBuffer)
    System.setOut(outputBuffer)
    System.setIn(inputBuffer)
  }

  @After
  fun restoreSystem() {
    System.setErr(stdErr)
    System.setIn(stdIn)
    System.setOut(stdOut)
  }

  @Test
  fun capture() {
    assertThat(captured.written()).isEmpty()
    IO.capture().use { io ->
      println("foo foo is on the loose")
      assertThat(io.captured.written()).isEqualTo("foo foo is on the loose\n")
    }
    assertThat(captured.written()).isEmpty()
  }
}
