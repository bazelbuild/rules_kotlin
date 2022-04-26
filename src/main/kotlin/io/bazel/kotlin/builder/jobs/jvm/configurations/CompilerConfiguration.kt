package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.Artifact
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

fun interface CompilerConfiguration {
  fun K2JVMCompilerArguments.configure(context: JobContext)

  fun applyTo(arguments: K2JVMCompilerArguments, context: JobContext): K2JVMCompilerArguments {
    return arguments.apply { configure(context) }
  }

  operator fun plus(that: CompilerConfiguration): CompilerConfiguration {
    return CompilerConfiguration { context ->
      applyTo(this, context)
      that.applyTo(this, context)
    }
  }

  fun K2JVMCompilerArguments.plugin(
    id: String,
    classpath: List<Artifact>,
    vararg arguments: Pair<String, String>,
    jobContext: JobContext
  ) {
    plugins(
      classpath,
      arguments.map { (name, value) ->
        "plugin:${id}:$name=$value"
      },
      jobContext
    )
  }

  fun K2JVMCompilerArguments.plugins(
    classpath: List<Artifact>,
    arguments: List<String>,
    jobContext: JobContext
  ) {
    val dirTokens = mapOf(
      "{generatedClasses}" to jobContext.generatedClasses.toString(),
      "{stubs}" to jobContext.stubs.toString(),
      "{generatedSources}" to jobContext.generatedSources.toString()
    )
    pluginClasspaths += classpath.map { it.path.toString() }
    pluginOptions += arguments.map { argument ->
      "plugin:" + dirTokens.entries.fold(argument) { formatting, (token, value) ->
        formatting.replace(token, value)
      }
    }
  }

  operator fun <String> Array<String>?.plus(other: Array<String>): Array<String> = when (this) {
    null -> other
    else -> plus(other)
  }

  operator fun Array<String>?.plus(other: List<String>): Array<String> = plus(other.toTypedArray())

}
