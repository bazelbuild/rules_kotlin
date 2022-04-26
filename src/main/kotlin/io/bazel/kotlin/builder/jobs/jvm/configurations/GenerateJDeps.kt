package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.Artifact
import io.bazel.kotlin.builder.jobs.jvm.CompilationInputs
import io.bazel.kotlin.builder.jobs.jvm.CompilationOutputs
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

class GenerateJDeps(
  val jdeps: Artifact,
  val jdepId: String,
  val input: CompilationInputs,
  val output: CompilationOutputs
) : CompilerConfiguration {

  override fun K2JVMCompilerArguments.configure(context:JobContext) {
    output.outputJdeps?.run {
      plugin(
        jdepId,
        listOf(jdeps),
        "output" to path.toString(),
        "target_label" to input.targetLabel,
        "strict_kotlin_deps" to input.strictKotlinDeps.toString(),
        *input.directDependencies.map { "direct_dependencies" to it.path.toString() }.toTypedArray(),
        jobContext = context
      )
    }
  }
}
