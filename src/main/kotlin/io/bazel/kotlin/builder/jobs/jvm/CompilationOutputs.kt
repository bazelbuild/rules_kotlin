package io.bazel.kotlin.builder.jobs.jvm

import java.nio.file.Path

interface CompilationOutputs {
  val outputSrcJar: Path?
  val output: Path?
  val outputJdeps: Path?
  val outputJsJar: Path?
}
