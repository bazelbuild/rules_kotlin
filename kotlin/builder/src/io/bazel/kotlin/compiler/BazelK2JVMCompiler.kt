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
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services

@Suppress("unused")
class BazelK2JVMCompiler(private val delegate: K2JVMCompiler = K2JVMCompiler()) {
    private fun createArgs(args: Array<out String>): K2JVMCompilerArguments {
        var friendsPaths: Array<String>? = null

        val tally = mutableListOf<String>()
        var i = 0
        do {
            when {
            // https://github.com/bazelbuild/rules_kotlin/issues/69: remove once jetbrains adds a flag for it.
                args[i].startsWith("--friend-paths") -> {
                    i++
                    friendsPaths = args[i].split(":").toTypedArray()
                }
                else -> tally += args[i]
            }
            i++
        } while (i < args.size)

        return delegate.createArguments().also {
            delegate.parseArguments(tally.toTypedArray(), it)
            if (friendsPaths != null) {
                it.friendPaths = friendsPaths
            }
        }
    }

    fun exec(errStream: java.io.PrintStream, vararg args: kotlin.String): ExitCode {
        val arguments = createArgs(args)
        val collector = PrintingMessageCollector(errStream, MessageRenderer.PLAIN_RELATIVE_PATHS, arguments.verbose)
        return delegate.exec(collector, Services.EMPTY, arguments)
    }
}