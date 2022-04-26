package io.bazel.kotlin.builder.jobs.jvm.flags

import io.bazel.kotlin.builder.jobs.jvm.Artifact
import io.bazel.kotlin.builder.utils.Arguments

interface ArtifactInputs : FilesSystemInputs {

  fun Arguments.artifact(name: String, description: String, required: Boolean = false) =
    flag<Artifact>(name, description, required = required) {
      Artifact(fileSystem.getPath(toString()))
    }

  fun Arguments.artifact(name: String, description: String, default:String) =
    flag(name, description, required = true, default = Artifact(fileSystem.getPath(default))) {
      Artifact(fileSystem.getPath(toString()))
    }

  fun Arguments.artifactList(name: String, description: String, required: Boolean = false) =
    flag<List<Artifact>>(
      name,
      description,
      emptyList(),
      required
    ) {
      split(",").map { Artifact(fileSystem.getPath(it)) }
    }
}
