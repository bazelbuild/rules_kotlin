package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler

//TODO: Support kotlin diagnostics
class BazelK2JSCompiler : K2JSCompiler() {
  fun exec(errStream: java.io.PrintStream, @Suppress("UNUSED_PARAMETER") diagnosticsFile: String?, vararg args: kotlin.String): ExitCode {
    return exec(errStream, *args)
  }
}
