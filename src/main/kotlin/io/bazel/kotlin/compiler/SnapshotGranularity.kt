package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity

enum class SnapshotGranularity {
  CLASS_LEVEL,
  CLASS_MEMBER_LEVEL,
  ;

  val toClassSnapshotGranularity: ClassSnapshotGranularity
    get() =
      when (this) {
        CLASS_LEVEL -> ClassSnapshotGranularity.CLASS_LEVEL
        CLASS_MEMBER_LEVEL -> ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
      }
}
