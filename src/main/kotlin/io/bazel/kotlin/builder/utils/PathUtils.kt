package io.bazel.kotlin.builder.utils

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.exists

/**
 * Try to relativize this path to be relative to current working directory. Original path is
 * returned as-is if relativization can't be done.
 * Ensures returned path exists on the file system
 */
fun String.relativizeToPwd(): String {
  val pwd = System.getenv("PWD") ?: ""
  if (startsWith(pwd)) {
    val rpath = replace(pwd, "")
    if (File(rpath).exists()) {
      return rpath
    }
  }
  // Handle cases where path is in runfiles.
  // In this case relativize from current directory to runfiles dir
  val runfilesRPath = Paths.get(".").absolute().relativize(Paths.get(this).absolute())
  if (runfilesRPath.exists()) {
    return runfilesRPath.toString()
  }
  return this
}
