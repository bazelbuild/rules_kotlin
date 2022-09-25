package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class CompileWithPlugins<IN: CompileWithPlugins.In, OUT: CompileWithPlugins.Out> :
  PluginsConfiguration<IN, OUT> {
  interface In : PluginsConfiguration.In {
    val compilerPluginOptions: List<String>
    val compilerPluginClasspath : List<Path>
  }

  interface Out : PluginsConfiguration.Out

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    plugins(context.inputs.compilerPluginClasspath, context.inputs.compilerPluginOptions, context)
  }
}
