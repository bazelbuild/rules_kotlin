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

import com.google.common.collect.ImmutableMap
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.file.Paths


/** Utility class for getting runfiles in tests on Windows.  */
object BazelRunFiles {
    /**
     * Populated on windows. The RUNFILES_MANIFEST_FILE is set on platforms other then windows but it can be empty,]
     */
    private val manifestFile: String? =
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            checkNotNull(System.getenv("RUNFILES_MANIFEST_FILE"))
        } else null

    private val javaRunFiles = Paths.get(System.getenv("JAVA_RUNFILES"))

    private val runfiles by lazy {
        with(ImmutableMap.builder<String, String>()) {
            FileInputStream(manifestFile)
                .bufferedReader(Charset.forName("UTF-8"))
                .lines()
                .forEach { it ->
                    val line = it.trim { it <= ' ' }
                    if (!line.isEmpty()) {
                        // TODO(bazel-team): This is buggy when the path contains spaces, we should fix the manifest format.
                        line.split(" ").also {
                            check(it.size == 2) { "RunFiles manifest entry contains more than one space" }
                            put(it[0], it[1])
                        }
                    }
                }
            build()
        }
    }

    /**
     * Resolve a run file on windows or *nix.
     */
    fun resolveVerified(vararg path: String): File {
        return manifestFile?.let { mf ->
            path.joinToString("/").let { rfPath ->
                File(
                    checkNotNull(runfiles[rfPath]) {
                        "windows runfile manifest ${mf} did not contain path $rfPath"
                    }
                )
            }.also {
                check(it.exists()) { "runfile file $it did not exist" }
            }
        } ?: javaRunFiles.resolveVerified(*path)
    }
}

