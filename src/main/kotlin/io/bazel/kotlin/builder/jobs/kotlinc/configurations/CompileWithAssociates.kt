package io.bazel.kotlin.builder.jobs.kotlinc.configurations

import io.bazel.kotlin.builder.jobs.kotlinc.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path

class CompileWithAssociates<IN : CompileWithAssociates.In, OUT : CompileWithAssociates.Out> :
  CompilerConfiguration<IN, OUT> {


  interface In : CompileKotlinForJvm.In {
    val associatePaths : List<Path>
  }

  interface Out : CompileKotlinForJvm.Out

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    friendPaths += context.inputs.associatePaths.map(Path::toString)
  }

}
