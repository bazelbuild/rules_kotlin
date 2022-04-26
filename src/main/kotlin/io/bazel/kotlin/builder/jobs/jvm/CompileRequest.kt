package io.bazel.kotlin.builder.jobs.jvm

import java.io.File
import java.nio.file.Path

data class CompileRequest(
  val sources:Sources,
  val classpath:Classpath,
) {
  data class Sources(val directories:List<Path>)
  data class Classpath(val jars:List<Path>)
}
