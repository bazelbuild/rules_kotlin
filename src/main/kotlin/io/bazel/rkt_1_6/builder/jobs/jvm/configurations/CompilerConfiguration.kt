package io.bazel.rkt_1_6.builder.jobs.jvm.configurations

import io.bazel.rkt_1_6.builder.jobs.jvm.JobContext
import io.bazel.rkt_1_6.builder.jobs.jvm.MoreArrays
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

fun interface CompilerConfiguration<INPUTS, OUTPUTS> {
  fun K2JVMCompilerArguments.configure(context: JobContext<INPUTS, OUTPUTS>)

  fun applyTo(arguments: K2JVMCompilerArguments, context: JobContext<INPUTS, OUTPUTS>): K2JVMCompilerArguments {
    return arguments.apply { configure(context) }
  }


  fun packageArtifacts(context: JobContext<INPUTS, OUTPUTS>) {}

  operator fun plus(that: CompilerConfiguration<INPUTS, OUTPUTS>): CompilerConfiguration<INPUTS, OUTPUTS> {
    return CompilerConfiguration { context ->
      applyTo(this, context)
      that.applyTo(this, context)
    }
  }

  operator fun Array<String>?.plus(other: Array<String>): Array<String> = when (this) {
    null -> other
    else -> MoreArrays.concatenate(this, other)
  }

  operator fun Array<String>?.plus(other: List<String>): Array<String> = plus(other.toTypedArray())

  val Path.extension get() = fileName.toString().substringAfterLast('.')

  fun <T> Path.walk(action: Stream<Path>.()->T) : T = Files.walk(this).use(action)

}
