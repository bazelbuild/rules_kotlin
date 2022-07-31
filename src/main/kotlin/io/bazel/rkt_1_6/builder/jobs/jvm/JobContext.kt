package io.bazel.rkt_1_6.builder.jobs.jvm

import io.bazel.worker.ContextLog.FileScope
import io.bazel.worker.ContextLog.Logging
import java.nio.file.Files.createDirectories
import java.nio.file.Path

class JobContext<INPUTS, OUTPUTS> private constructor(scope: InnerContext<*>,
                                                      val inputs: INPUTS,
                                                      val outputs: OUTPUTS) : Logging by scope,
  FileScope by scope {

  companion object {
    fun <T, INPUTS, OUTPUTS> of(scope: T, inputs: INPUTS, outputs:OUTPUTS): JobContext<INPUTS, OUTPUTS>
      where T : Logging, T : FileScope {
      return JobContext(InnerContext(scope), inputs, outputs)
    }
    private class InnerContext<T>(val scope: T) : Logging by scope, FileScope by scope
      where T : Logging, T : FileScope
  }

  val generatedResources by lazy {
    createDirectories(directory.resolve("gen-res"))
  }

  val generatedClasses by lazy {
    createDirectories(directory.resolve("gen-cls"))
  }

  val generatedSources by lazy {
    createDirectories(directory.resolve("gen-srcs"))
  }

  val stubs by lazy {
    createDirectories(directory.resolve("stubs"))
  }

  val classes by lazy {
    createDirectories(directory.resolve("classes"))
  }

  val incremental: Path by lazy {
    createDirectories(directory.resolve("incremental"))
  }
}
