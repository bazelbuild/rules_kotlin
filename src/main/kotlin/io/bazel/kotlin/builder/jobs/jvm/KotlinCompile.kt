package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.kotlin.builder.jobs.jvm.configurations.CompilerConfiguration
import io.bazel.worker.Status
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.INTERNAL_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.ExitCode.SCRIPT_EXECUTION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.PlainTextMessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.file.File.Companion.separator

class KotlinCompile {
  private val compiler = K2JVMCompiler()
  fun run(
    job: JobContext,
    configurations: List<CompilerConfiguration>,
  ): Status {

    val configuration = configurations.fold(K2JVMCompilerArguments()) { arg, cfg ->
      cfg.applyTo(arg, job)
    }

    val renderer = object : PlainTextMessageRenderer() {
      private val sourceRoot =
        configuration.freeArgs.takeIf { it.size > 1 }
          ?.reduce(String::commonPrefixWith)
          ?.substringBeforeLast(separator)
          ?: ""

      override fun getName(): String = "RelativePath"
      override fun getPath(location: CompilerMessageSourceLocation): String =
        location.path.removePrefix(sourceRoot)
    }
    
    return when (compiler.exec(
      Report(job, renderer),
      Services.EMPTY,
      configuration
    )) {
      OK -> Status.SUCCESS
      COMPILATION_ERROR -> Status.ERROR
      INTERNAL_ERROR -> Status.INTERNAL_ERROR
      SCRIPT_EXECUTION_ERROR -> Status.ERROR
    }
  }
}
