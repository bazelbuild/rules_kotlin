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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.PrintStream
import java.nio.file.Paths
import io.bazel.kotlin.model.diagnostics.Diagnostics
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


@Suppress("unused")
class BazelK2JVMCompiler(private val delegate: K2JVMCompiler = K2JVMCompiler()) {
  fun exec(errStream: PrintStream, diagnosticsFile: String? = null, vararg args: String): ExitCode {
    val arguments = delegate.createArguments().also { delegate.parseArguments(args, it) }
    val collector =
      MessageCollectorWithDiagnostics(errStream, MessageRenderer.PLAIN_RELATIVE_PATHS, arguments.verbose)
    val exitCode = delegate.exec(collector, Services.EMPTY, arguments)


    diagnosticsFile?.let {
      try {
        val path = Paths.get(it)
        collector.writeTo(path)
      } catch (e: Throwable) {
      }
    }

    return exitCode
  }

  class MessageCollectorWithDiagnostics(errStream: PrintStream, messageRenderer: MessageRenderer, verbose: Boolean) : PrintingMessageCollector(errStream, messageRenderer, verbose) {
    private val builder = mutableMapOf<String, MutableList<Diagnostics.Diagnostic>>()

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
      super.report(severity, message, location)
      val builder = Diagnostics.Diagnostic
        .newBuilder()
      var path = ""
      location?.let {
        builder.range = convertRange(it)
        path = it.path
      }

      val diagnostic: Diagnostics.Diagnostic = builder
        .setSeverity(convertSeverity(severity))
        .setMessage(message)
        .build()


      this.builder.getOrPut(path) { mutableListOf() }
        .add(diagnostic)
    }

    private fun convertSeverity(severity: CompilerMessageSeverity): Diagnostics.Severity {
      return when (severity) {
        EXCEPTION -> Diagnostics.Severity.ERROR
        ERROR -> Diagnostics.Severity.ERROR
        STRONG_WARNING -> Diagnostics.Severity.WARNING
        WARNING -> Diagnostics.Severity.WARNING
        INFO -> Diagnostics.Severity.INFORMATION
        LOGGING -> Diagnostics.Severity.INFORMATION
        OUTPUT -> Diagnostics.Severity.INFORMATION
      }
    }

    private fun convertRange(location: CompilerMessageLocation): Diagnostics.Range {
      return Diagnostics.Range
        .newBuilder()
        .setStart(
          Diagnostics.Position
            .newBuilder()
            .setLine(location.line - 1)
            .setCharacter(location.column - 1)
            .build()
        )
        .build()
    }

    fun writeTo(path: Path) {
      val targetDiagnostics: Diagnostics.TargetDiagnostics.Builder = Diagnostics.TargetDiagnostics.newBuilder()

      targetDiagnostics.addAllDiagnostics(
        builder.map { (path, diagnostics) ->
          Diagnostics.FileDiagnostics
            .newBuilder()
            .setPath(path)
            .addAllDiagnostics(diagnostics)
            .build()
        })

      Files.write(path, targetDiagnostics.build().toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
  }
}
