package io.bazel.rkt_1_6.builder.jobs.jvm

import java.nio.file.Path

interface CompilationOutputs {
  val outputSrcJar: Path?
  val output: Path?
  val outputJdeps: Path?
  val outputJsJar: Path?
}
