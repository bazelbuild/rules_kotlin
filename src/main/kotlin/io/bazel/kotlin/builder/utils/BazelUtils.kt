/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder.utils

import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.Collections

/** Utility class for getting runfiles on windows and *nix.  */
object BazelRunFiles {
  private val isWindows = System.getProperty("os.name").lowercase().indexOf("win") >= 0

  /**
   * Set depending on --enable_runfiles, which defaults to false on Windows and true otherwise.
   */
  private val manifestFile: String? =
      if (isWindows) {
        System.getenv("RUNFILES_MANIFEST_FILE")
      } else null

  private val javaRunFiles = Paths.get(System.getenv("JAVA_RUNFILES"))

  private val runfiles by lazy {
    with(mutableMapOf<String, String>()) {
      FileInputStream(manifestFile)
          .bufferedReader(Charset.forName("UTF-8"))
          .lines()
          .forEach { it ->
            val line = it.trim { it <= ' ' }
            if (!line.isEmpty()) {
              // TODO(bazel-team): This is buggy when the path contains spaces, we should fix the manifest format.
              line.split(" ").also {
                check(it.size == 2) { "RunFiles manifest entry $line contains more than one space" }
                put(it[0], it[1])
              }
            }
          }
      Collections.unmodifiableMap(this)
    }
  }

  /**
   * Resolve a run file on windows or *nix.
   */
  @JvmStatic
  fun resolveVerified(vararg path: String): File {
    check(path.isNotEmpty())
    val fromManifest = manifestFile?.let { mf ->
      path.joinToString("/").let { rfPath ->
        // trim off the external/ prefix if it is present.
        val trimmedPath =
            if (rfPath.startsWith("external/")) {
              rfPath.replaceFirst("external/", "")
            } else {
              rfPath
            }
        File(
            checkNotNull(runfiles[trimmedPath]) {
              "runfile manifest $mf did not contain path mapping for $rfPath"
            }
        )
      }.also {
        check(it.exists()) { "file $it resolved from runfiles did not exist" }
      }
    }
    if (fromManifest != null) {
      return fromManifest
    }

    /// if it could not be resolved from the manifest then first try to resolve it directly.
    val resolvedDirect = File(path.joinToString(File.separator)).takeIf { it.exists() }
    if (resolvedDirect != null) {
      return resolvedDirect
    }

    // Try the java runfiles as the last resort.
    return javaRunFiles.resolveVerified(path.joinToString(File.separator))
  }
}
