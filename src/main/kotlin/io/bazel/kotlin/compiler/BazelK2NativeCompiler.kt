/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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

import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.config.Services

@Suppress("unused")
class BazelK2NativeCompiler {
  fun exec(
    errStream: java.io.PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")
    val konanHome = System.getenv("KONAN_HOME")
    requireNotNull(konanHome) { "KONAN_HOME env var must be set!" }

    System.setProperty("konan.home", konanHome)

    val delegate = K2Native()
    val arguments = delegate.createArguments().also { delegate.parseArguments(args, it) }
    val collector =
      PrintingMessageCollector(errStream, MessageRenderer.PLAIN_RELATIVE_PATHS, arguments.verbose)
    return delegate.exec(collector, Services.EMPTY, arguments)
  }
}
