package io.bazel.kotlin.builder.jobs.kotlinc.configurations

import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.jobs.kotlinc.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path
import kotlin.io.path.createDirectories

class GenerateAbi<IN : GenerateAbi.In, OUT : GenerateAbi.Out> : PluginsConfiguration<IN, OUT> {

  interface In : PluginsConfiguration.In, CompileKotlinForJvm.In {
    val abi: Path
  }

  interface Out : PluginsConfiguration.Out, CompileKotlinForJvm.Out {
    val abiJar: Path?
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    context.outputs.abiJar?.let {
      plugin(
        "org.jetbrains.kotlin.jvm.abi",
        listOf(context.inputs.abi),
        "outputDir" to context.incremental.resolve("abi").createDirectories().toString(),
        jobContext = context
      )
    }
  }

  override fun packageArtifacts(context: JobContext<IN, OUT>) {
    context.outputs.abiJar?.let { jar ->
      JarCreator(jar, normalize = true).use { archive ->
        archive.addDirectory(context.incremental.resolve("abi"))
      }
    }
  }
}
