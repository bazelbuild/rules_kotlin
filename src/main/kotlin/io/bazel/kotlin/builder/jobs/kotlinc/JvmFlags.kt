package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.Path

class JvmFlags(
  argument: Arguments,
  workingDirectory: Path,
) : JvmCoreFlags(
  argument,
  workingDirectory,
)
