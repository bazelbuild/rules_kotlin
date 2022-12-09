package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import io.bazel.kotlin.builder.jobs.kotlinc.JvmCoreFlags

class JvmFlags(
  argument: Arguments,
  workingDirectory: Path,
) : CoreJvmFlags(
  argument: Arguments,
  workingDirectory: Path,
)
