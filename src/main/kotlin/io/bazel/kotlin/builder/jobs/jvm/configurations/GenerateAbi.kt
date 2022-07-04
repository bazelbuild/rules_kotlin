package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class GenerateAbi(val inputs: Inputs, val outputs: Outputs) : CompilerConfiguration {

  interface Inputs {
    val abi : Path
  }

  interface Outputs {
    val outputAbiJar : Path?
  }


  override fun K2JVMCompilerArguments.configure(context: JobContext) {
    plugin(
      "org.jetbrains.kotlin.jvm.abi",
      listOf(inputs.abi),
      "outputDir" to outputs.outputAbiJar.toString(),
      jobContext = context
    )
  }
}
