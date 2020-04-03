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
package io.bazel.kotlin.builder.utils.jars

import java.nio.file.Path
import java.util.function.Predicate

class SourceJarExtractor(destDir: Path, val fileMatcher: Predicate<String> = Predicate { true }) :
  JarExtractor(destDir) {
  val jarFiles = mutableListOf<Path>()
  val sourcesList = mutableListOf<String>()

  override fun preWrite(isDirectory: Boolean, target: Path): Boolean {
    if (!isDirectory && fileMatcher.test(target.toString())) {
      sourcesList.add(target.toString())
    }
    return true
  }

  fun execute() {
    destDir.also {
      try {
        it.toFile().mkdirs()
      } catch (ex: Exception) {
        throw RuntimeException("could not create unpack directory at $it", ex)
      }
    }
    jarFiles.forEach {
      try {
        extract(it)
      } catch (ex: Throwable) {
        throw RuntimeException("error extracting source jar $it", ex)
      }
    }
  }
}
