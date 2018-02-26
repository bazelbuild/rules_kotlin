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
package io.bazel.kotlin.builder

/**
 * Interface for command line programs.
 *
 * This is the same thing as a main function, except not static.
 */
interface CommandLineProgram {
    /**
     * Runs blocking program start to finish.
     *
     * This function might be called multiple times throughout the life of this object. Output
     * must be sent to [System.out] and [System.err].
     *
     * @param args command line arguments
     * @return program exit code, i.e. 0 for success, non-zero for failure
     */
    fun apply(args: List<String>): Int

    abstract class Base(
            protected val flags: FlagNameMap = emptyMap()
    ): CommandLineProgram {
        private val mandatoryFlags: List<Flag> = flags.values.filter { it is Flag.Mandatory }

        private fun createContext(toolchain: KotlinToolchain, args: List<String>): Context {
            val tally = mutableMapOf<Flag, String>()

            if (args.size % 2 != 0) {
                throw RuntimeException("args should be k,v pairs")
            }

            for (i in 0 until args.size / 2) {
                val flag = args[i * 2]
                val value = args[i * 2 + 1]
                val field = flags[flag] ?: throw RuntimeException("unrecognised arg: " + flag)
                tally[field] = value
            }

            mandatoryFlags.forEach { require(tally.containsKey(it)) { "missing mandatory flag ${it.globalFlag}" } }
            return Context(toolchain, tally)
        }

        abstract val toolchain: KotlinToolchain
        abstract fun actions(toolchain: KotlinToolchain, ctx: Context): List<BuildAction>

        override fun apply(args: List<String>): Int {
            val ctx = createContext(toolchain, args)
            var exitCode = 0
            for (action in actions(toolchain,ctx)) {
                exitCode = action(ctx)
                if (exitCode != 0)
                    break
            }
            return exitCode
        }
    }
}