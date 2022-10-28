package io.bazel.kotlin.builder.jobs.kotlinc.configurations

import io.bazel.kotlin.builder.jobs.kotlinc.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Files
import java.nio.file.Path

interface PluginsConfiguration<IN : PluginsConfiguration.In, OUT : PluginsConfiguration.Out>
  : CompilerConfiguration<IN, OUT> {

  interface In

  interface Out

  fun K2JVMCompilerArguments.plugin(
    id: String,
    classpath: List<Path>,
    vararg arguments: Pair<String, String>,
    jobContext: JobContext<IN, OUT>
  ) {
    plugins(
      classpath,
      arguments.map { (name, value) ->
        "${id}:$name=$value"
      },
      jobContext
    )
  }

  fun K2JVMCompilerArguments.plugins(
    classpath: List<Path>,
    arguments: List<String>,
    jobContext: JobContext<IN, OUT>
  ) {
    val dirTokens = mapOf(
      "{generatedClasses}" to jobContext.generatedClasses.toString(),
      "{stubs}" to jobContext.stubs.toString(),
      "{generatedSources}" to jobContext.generatedSources.toString(),
      "{temp}" to Files.createTempDirectory(jobContext.incremental, "plugin").toString(),
      "{resources}" to jobContext.generatedResources.toString(),
    )
    pluginClasspaths += classpath.map { it.toString() }
    pluginOptions += arguments.map { argument ->
      "plugin:" + dirTokens.entries.fold(argument) { formatting, (token, value) ->
        formatting.replace(token, value)
      }
    }
  }
}

