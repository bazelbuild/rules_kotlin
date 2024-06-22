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
package io.bazel.kotlin.builder.tasks.jvm

import com.google.devtools.build.lib.view.proto.Deps
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

object JDepsGenerator {
  internal fun writeJdeps(
    path: String,
    jdepsContent: Deps.Dependencies,
  ) {
    Paths.get(path).also {
      Files.deleteIfExists(it)
      FileOutputStream(Files.createFile(it).toFile()).use(jdepsContent::writeTo)
    }
  }

  internal fun emptyJdeps(label: String): Deps.Dependencies =
    Deps.Dependencies.newBuilder().let {
      it.ruleLabel = label
      it.build()
    }
}
