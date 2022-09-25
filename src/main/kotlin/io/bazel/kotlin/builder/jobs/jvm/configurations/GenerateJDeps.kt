package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class GenerateJDeps<IN: GenerateJDeps.In, OUT: GenerateJDeps.Out> : PluginsConfiguration<IN, OUT> {

  interface In : PluginsConfiguration.In {
    val jdeps: Path
    val targetLabel: String
    val strictKotlinDeps: Boolean
    val directDependencies: List<Path>
  }

  interface Out : PluginsConfiguration.Out {
    val outputJdeps: Path?
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    context.outputs.outputJdeps?.run {
      plugin(
        "io.bazel.kotlin.plugin.jdeps.JDepsGen",
        listOf(context.inputs.jdeps),
        "output" to toString(),
        "target_label" to context.inputs.targetLabel,
        "strict_kotlin_deps" to context.inputs.strictKotlinDeps.toString(),
        *context.inputs.directDependencies.map { "direct_dependencies" to it.toString() }.toTypedArray(),
        jobContext = context
      )
    }
  }
}
