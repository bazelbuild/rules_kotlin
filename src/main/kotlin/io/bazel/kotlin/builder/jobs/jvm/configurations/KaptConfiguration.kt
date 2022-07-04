package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.Base64
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path
import kotlin.io.path.createDirectories

class KaptConfiguration(val inputs: Inputs) : CompilerConfiguration {
  enum class AptMode {
    stubs,
    apt,
    stubsAndApt;
  }

  interface Inputs {
    val processorPath: List<Path>
    val processors: List<Path>
    val kapt: Path
    val verbose: Boolean
    val kaptMode: AptMode
    val jvmTarget: String
    val processorOptions: Map<String, String>
  }

  private val Inputs.pluginClassPath: List<Path> get() = (processorPath + kapt)

  override fun K2JVMCompilerArguments.configure(context: JobContext) {
    plugin(
      "org.jetbrains.kotlin.kapt3", inputs.pluginClassPath,
      "sources" to context.generatedSources.toString(),
      "classes" to context.generatedClasses.toString(),
      "stubs" to context.stubs.toString(),
      "incrementalData" to context.incremental.resolve("kapt").createDirectories().toString(),
      "javacArguments" to Base64.encode(
        mapOf(
          "-source" to inputs.jvmTarget,
          "-target" to inputs.jvmTarget
        )
      ),
      "correctErrorTypes" to "false",
      "verbose" to verbose.toString(),
      "aptMode" to inputs.kaptMode.name,
      "apoptions" to Base64.encode(inputs.processorOptions),
      *inputs.processors.map { "apclasspath" to it.toString() }.toTypedArray(),
      jobContext = context,
    )
  }
}
