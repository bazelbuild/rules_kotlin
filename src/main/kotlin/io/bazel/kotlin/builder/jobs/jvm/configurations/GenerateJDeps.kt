package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class GenerateJDeps(
  val jdeps: Path, val input: Inputs, val output: Outputs
) : CompilerConfiguration {

  interface Inputs {
    val jdeps: Path
    val targetLabel: String
    val strictKotlinDeps: Boolean
    val directDependencies: List<Path>
  }

  interface Outputs {
    val outputJdeps: Path?
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext) {
    output.outputJdeps?.run {
      plugin(
        "io.bazel.kotlin.plugin.jdeps.JDepsGen",
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
