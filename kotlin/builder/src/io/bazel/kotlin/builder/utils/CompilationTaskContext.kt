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
package io.bazel.kotlin.builder.utils


import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.TextFormat
import io.bazel.kotlin.model.CompilationTaskInfo
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths

class CompilationTaskContext(val info: CompilationTaskInfo, private val out: PrintStream) {
    private val executionRoot: String = Paths.get("").toAbsolutePath().toString() + File.separator
    private val timings = mutableListOf<String>()
    val isDebug: Boolean = info.debug > 0

    /**
     * Print debugging messages if it is enabled for the task.
     */
    fun debugPrint(msg: String) {
        if (info.debug > 0) {
            out.println(msg)
        }
    }

    /**
     * Print a debugging message if it debugging is enabled for the task. The lines are tab seperated.
     */
    private inline fun debugPrintHeadedLines(header: String, lines: () -> String) {
        if (info.debug > 0) {
            out.println(if (header.endsWith(":")) header else "$header:")
            out.print("|  ${lines().replace("\n", "\n|  ")}")
        }
    }

    /**
     * Print a proto message if debugging is enabled for the task.
     */
    fun debugPrintProto(header: String, msg: MessageOrBuilder) {
        debugPrintHeadedLines(header) { TextFormat.printToString(msg) }
    }

    fun printCompilerOutput(line: String) {
        out.println(trimExecutionRootPrefix(line))
    }

    fun printCompilerOutput(lines: List<String>) {
        lines.map(::trimExecutionRootPrefix).forEach(out::println)
    }

    private fun trimExecutionRootPrefix(toPrint: String): String {
        // trim off the workspace component
        return if (toPrint.startsWith(executionRoot)) {
            toPrint.replaceFirst(executionRoot, "")
        } else toPrint
    }

    fun <T> execute(name: String, task: () -> T): T {
        val start = System.currentTimeMillis()
        return try {
            task()
        } finally {
            val stop = System.currentTimeMillis()
            timings += "$name: ${stop - start} ms"
        }
    }
}
