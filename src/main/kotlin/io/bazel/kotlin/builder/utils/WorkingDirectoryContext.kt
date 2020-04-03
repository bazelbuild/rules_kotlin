/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.builder.utils

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

/** WorkingDirectoryContext provides a consistent base directory that is removed on close. */
class WorkingDirectoryContext(val dir: Path) : Closeable {
  companion object {
    fun newContext(): WorkingDirectoryContext = WorkingDirectoryContext(
        Files.createTempDirectory("working"))
  }

  override fun close() {
    Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
  }
}
