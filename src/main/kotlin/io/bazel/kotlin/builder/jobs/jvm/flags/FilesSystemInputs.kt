package io.bazel.kotlin.builder.jobs.jvm.flags

import java.nio.file.FileSystem

interface FilesSystemInputs {
  val fileSystem : FileSystem
}
