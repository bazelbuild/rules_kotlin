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
package io.bazel.kotlin.builder.tasks.jvm


import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class KotlinCompilerOutputSink @Inject constructor(
    private val streamProvider: Provider<PrintStream>
) {
    private val executionRoot: String = Paths.get("").toAbsolutePath().toString() + File.separator

    fun deliver(line: String) {
        streamProvider.get().println(trimExecutionRootPrefix(line))
    }

    fun deliver(lines: List<String>) {
        streamProvider.get().also { stream -> lines.map(::trimExecutionRootPrefix).forEach(stream::println) }
    }

    private fun trimExecutionRootPrefix(toPrint: String): String {
        // trim off the workspace component
        return if (toPrint.startsWith(executionRoot)) {
            toPrint.replaceFirst(executionRoot, "")
        } else toPrint
    }
}
