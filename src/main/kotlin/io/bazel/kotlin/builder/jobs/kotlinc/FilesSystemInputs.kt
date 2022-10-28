package io.bazel.kotlin.builder.jobs.kotlinc.flags

import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.FileSystem
import java.nio.file.Path

interface FilesSystemInputs {
  val fileSystem : FileSystem

  fun Arguments.path(name: String, description: String, required: Boolean = false) =
    flagOf<Path>(name, description, required = required) {
      fileSystem.getPath(toString())
    }

  fun Arguments.path(name: String, description: String, default:String) =
    flagOf(name, description, required = true, default = fileSystem.getPath(default)) {
      fileSystem.getPath(toString())
    }

  fun Arguments.paths(name: String, description: String, required: Boolean = false) =
    flagOf<List<Path>>(
      name,
      description,
      emptyList(),
      required
    ) {
      split(",").map { fileSystem.getPath(it) }
    }
}
