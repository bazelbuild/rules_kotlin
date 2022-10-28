package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.Base64
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

/**
 * [KaptConfiguration] defines the common kapt configuration.
 */
interface KaptConfiguration<IN : KaptConfiguration.In, OUT : KaptConfiguration.Out> :
  PluginsConfiguration<IN, OUT> {

  interface In : PluginsConfiguration.In {
    val processorPath: List<Path>
    val processors: List<String>
    val kapt: Path
    val verbose: Boolean
    val jvmTarget: String
    val processorOptions: Map<String, String>
  }

  interface Out : PluginsConfiguration.Out

  fun K2JVMCompilerArguments.configure(aptMode: String, context: JobContext<IN, OUT>) {
    plugin(
      "org.jetbrains.kotlin.kapt3",
      context.inputs.run { processorPath + listOf(kapt) },
      "sources" to context.generatedSources.toString(),
      "classes" to context.generatedClasses.toString(),
      "stubs" to context.stubs.toString(),
      "incrementalData" to context.generatedClasses.toString(),
      "javacArguments" to Base64.encode(
        "-source" to context.inputs.jvmTarget,
        "-target" to context.inputs.jvmTarget
      ),
      "correctErrorTypes" to "false",
      "verbose" to context.inputs.verbose.toString(),
      "aptMode" to aptMode,

      "apoptions" to Base64.encode(context.inputs.processorOptions),
      *context.inputs.processorPath.map { "apclasspath" to it.toString() }.toTypedArray(),
      *context.inputs.processors.map { "processors" to it }.toTypedArray(),
      jobContext = context,
    )
  }
}
