/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.PrintStream

/**
 * Logger that captures IC (incremental compilation) events for debugging/testing.
 *
 * When passed to [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation],
 * this logger receives IC-related messages including:
 * - Which files are being compiled in each iteration
 * - Why files were marked dirty (ABI changes, member changes, etc.)
 * - Compilation exit codes
 */
class ICLogger(
  private val out: PrintStream,
  override val isDebugEnabled: Boolean = true,
) : KotlinLogger {
  override fun error(
    msg: String,
    throwable: Throwable?,
  ) {
    out.println("[IC ERROR] $msg")
    throwable?.printStackTrace(out)
  }

  override fun warn(
    msg: String,
    throwable: Throwable?,
  ) {
    out.println("[IC WARN] $msg")
  }

  override fun info(msg: String) {
    out.println("[IC INFO] $msg")
  }

  override fun debug(msg: String) {
    out.println("[IC DEBUG] $msg")
  }

  override fun lifecycle(msg: String) {
    out.println("[IC] $msg")
  }
}
