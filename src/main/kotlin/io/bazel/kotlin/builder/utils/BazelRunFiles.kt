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

import com.google.devtools.build.runfiles.Runfiles
import java.io.File
import java.io.FileNotFoundException

/** Utility class for getting runfiles on windows and *nix.  */
object BazelRunFiles {

  private val runfiles by lazy {
    Runfiles.preload().unmapped()
  }

  /**
   * Resolve a run file on windows or *nix.
   */
  @JvmStatic
  fun resolveVerifiedFromProperty(key: String) =
    System.getProperty(key)
      ?.let { path -> runfiles.rlocation(path) }
      ?.let { File(it) }
      ?.also {
        if (!it.exists()) {
          throw IllegalStateException("$it does not exist in the runfiles!\n${System.getenv().entries.joinToString("\n\t") { (k,v) -> "$k: $v" }}")
        }
      }
      ?: let {
        throw FileNotFoundException("no reference for $key in ${System.getProperties()}")
      }
}
