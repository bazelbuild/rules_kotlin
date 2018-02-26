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
package io.bazel.kotlin.builder.mode.jvm.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths


/**
 * Utility class to perform common pre-processing on the compiler output before it is passed onto a delegate
 * PrintStream.
 */
// The kotlin compiler produces absolute file paths but the intellij plugin expects workspace root relative paths to
// render errors.
abstract class KotlinCompilerOutputProcessor private constructor(internal val delegate: PrintStream) {
    private val byteArrayOutputStream = ByteArrayOutputStream()
    // Get the absolute path to ensure the sandbox root is resolved.
    private val executionRoot = Paths.get("").toAbsolutePath().toString() + File.separator

    val collector: PrintStream = PrintStream(byteArrayOutputStream)

    class ForKotlinC(delegate: PrintStream) : KotlinCompilerOutputProcessor(delegate) {
        override fun processLine(line: String): Boolean {
            delegate.println(trimExecutionRootPrefix(line))
            return true
        }
    }

    internal fun trimExecutionRootPrefix(toPrint: String): String {
        // trim off the workspace component
        return if (toPrint.startsWith(executionRoot)) {
            toPrint.replaceFirst(executionRoot.toRegex(), "")
        } else toPrint
    }

    protected abstract fun processLine(line: String): Boolean

    fun process() {
        try {
            for (s in ByteArrayInputStream(byteArrayOutputStream.toByteArray()).bufferedReader().lineSequence()) {
                val shouldContinue = processLine(s)
                if (!shouldContinue) {
                    break
                }
            }
        } finally {
            delegate.flush()
        }
    }
}
