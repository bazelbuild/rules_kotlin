package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.worker.ContextLog.FileScope
import io.bazel.worker.ContextLog.Logging
import java.nio.file.Files
import java.nio.file.Files.createDirectories

class JobContext private constructor(scope: InnerContext<*>) : Logging by scope,
  FileScope by scope {

  companion object {
    fun <T> of(scope: T): JobContext
      where T : Logging, T : FileScope {
      return JobContext(InnerContext(scope))
    }
    private class InnerContext<T>(val scope: T) : Logging by scope, FileScope by scope
      where T : Logging, T : FileScope
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
}
