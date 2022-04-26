package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.kotlin.builder.jobs.jvm.flags.ArtifactInputs
import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.FileSystem
import java.nio.file.FileSystems

class CompilationOutputs(
  argument: Arguments,
  override val fileSystem: FileSystem = FileSystems.getDefault()
) : ArtifactInputs {
  val outputSrcJar by argument.artifact("kotlin_output_srcjar", "", required = true)
  val output by argument.artifact("output", "", required = true)
  val outputJdeps by argument.artifact("kotlin_output_jdeps", "", required = true)
  val outputJsJar by argument.artifact("kotlin_output_js_jar", "", required = true)
}
