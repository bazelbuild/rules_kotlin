package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.CompilationInputs
import io.bazel.kotlin.builder.jobs.jvm.CompilationOutputs
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class GenerateJDeps(
  val jdeps: Path,
  val jdepId: String,
  val input: CompilationInputs,
  val output: CompilationOutputs
) : CompilerConfiguration {

  override fun K2JVMCompilerArguments.configure(context:JobContext) {
    output.outputJdeps?.run {
      plugin(
        jdepId,
        listOf(jdeps),
        "output" to toString(),
        "target_label" to input.targetLabel,
        "strict_kotlin_deps" to input.strictKotlinDeps.toString(),
        *input.directDependencies.map { "direct_dependencies" to it.toString() }.toTypedArray(),
        jobContext = context
      )
    }
  }
}
