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

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class IO(
  val input: InputStream,
  val output: PrintStream,
  private val captured: ByteArrayOutputStream,
  private val restore: () -> Unit = {}
) : Closeable {

  /**
   * Reads the captured std out and err as a UTF-8 string and then resets the
   * captured ByteArrayOutputStream.
   *
   * Resetting the ByteArrayOutputStream prevents the worker from returning
   * the same console output multiple times
   **/
  fun readCapturedAsUtf8String(): String {
    captured.flush()
    val out = captured.toByteArray().toString(StandardCharsets.UTF_8)
    captured.reset()
    return out
  }

  companion object {
    fun capture(): IO {
      val stdErr = System.err
      val stdIn = BufferedInputStream(System.`in`)
      val stdOut = System.out
      val inputBuffer = ByteArrayInputStream(ByteArray(0))
      val captured = ByteArrayOutputStream()
      val outputBuffer = PrintStream(captured)

      // delegate the system defaults to capture execution information
      System.setErr(outputBuffer)
      System.setOut(outputBuffer)
      System.setIn(inputBuffer)

      return IO(stdIn, stdOut, captured) {
        System.setOut(stdOut)
        System.setIn(stdIn)
        System.setErr(stdErr)
      }
    }
  }

  override fun close() {
    restore.invoke()
  }
}
