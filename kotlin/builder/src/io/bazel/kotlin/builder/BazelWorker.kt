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


import com.google.devtools.build.lib.worker.WorkerProtocol
import io.bazel.kotlin.builder.utils.rootCause
import java.io.*
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

sealed class ToolException(
    msg: String,
    ex: Throwable? = null
) : RuntimeException(msg, ex)

class CompilationException(msg: String, cause: Throwable? = null) :
    ToolException(msg, cause)

class CompilationStatusException(
    msg: String,
    val status: Int,
    val lines: List<String> = emptyList()
) : ToolException("$msg:${lines.joinToString("\n", "\n")}")


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
}


/**
 * Bazel worker runner.
 *
 * This class adapts a traditional command line program so it can be spawned by Bazel as a
 * persistent worker process that handles multiple invocations per JVM. It will also be backwards
 * compatible with being run as a normal single-invocation command.
 *
 * @param <T> delegate program type
</T> */
class BazelWorker(
    private val delegate: CommandLineProgram,
    private val output: PrintStream,
    private val mnemonic: String
) : CommandLineProgram {
    companion object {
        private const val INTERUPTED_STATUS = 0
        private const val ERROR_STATUS = 1
    }

    override fun apply(args: List<String>): Int {
        return if (args.contains("--persistent_worker"))
            runAsPersistentWorker(args)
        else delegate.apply(loadArguments(args, false))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun runAsPersistentWorker(ignored: List<String>): Int {
        val realStdIn = System.`in`
        val realStdOut = System.out
        val realStdErr = System.err
        try {
            ByteArrayInputStream(ByteArray(0)).use { emptyIn ->
                ByteArrayOutputStream().use { buffer ->
                    PrintStream(buffer).use { ps ->
                        System.setIn(emptyIn)
                        System.setOut(ps)
                        System.setErr(ps)
                        while (true) {
                            val request = WorkerProtocol.WorkRequest.parseDelimitedFrom(realStdIn) ?: return 0
                            var exitCode: Int

                            exitCode = try {
                                delegate.apply(loadArguments(request.argumentsList, true))
                            } catch (e: RuntimeException) {
                                if (wasInterrupted(e)) {
                                    return INTERUPTED_STATUS
                                }
                                System.err.println(
                                    "ERROR: Worker threw uncaught exception with args: " + request.argumentsList.stream().collect(Collectors.joining(" "))
                                )
                                e.printStackTrace(System.err)
                                ERROR_STATUS
                            }

                            WorkerProtocol.WorkResponse.newBuilder()
                                .setOutput(buffer.toString())
                                .setExitCode(exitCode)
                                .build()
                                .writeDelimitedTo(realStdOut)
                            realStdOut.flush()
                            buffer.reset()
                            System.gc()  // be a good little worker process and consume less memory when idle
                        }
                    }
                }
            }
        } catch (e: IOException) {
            if (wasInterrupted(e)) {
                return INTERUPTED_STATUS
            }
            throw e
        } catch (e: RuntimeException) {
            if (wasInterrupted(e)) {
                return INTERUPTED_STATUS
            }
            throw e
        } finally {
            System.setIn(realStdIn)
            System.setOut(realStdOut)
            System.setErr(realStdErr)
        }
        throw RuntimeException("drop through")
    }

    private fun loadArguments(args: List<String>, isWorker: Boolean): List<String> {
        if (args.isNotEmpty()) {
            val lastArg = args[args.size - 1]

            if (lastArg.startsWith("@")) {
                val pathElement = lastArg.substring(1)
                val flagFile = Paths.get(pathElement)
                if (isWorker && lastArg.startsWith("@@") || Files.exists(flagFile)) {
                    if (!isWorker && !mnemonic.isEmpty()) {
                        output.printf(
                            "HINT: %s will compile faster if you run: " + "echo \"build --strategy=%s=worker\" >>~/.bazelrc\n",
                            mnemonic, mnemonic
                        )
                    }
                    try {
                        return Files.readAllLines(flagFile, UTF_8)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }

                }
            }
        }
        return args
    }

    private fun wasInterrupted(e: Throwable): Boolean {
        val cause = e.rootCause
        if (cause is InterruptedException || cause is InterruptedIOException) {
            output.println("Terminating worker due to interrupt signal")
            return true
        }
        return false
    }
}
