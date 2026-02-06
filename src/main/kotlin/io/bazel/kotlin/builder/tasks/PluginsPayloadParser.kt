/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder.tasks

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.util.JsonFormat
import io.bazel.kotlin.model.JvmCompilationTask
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object PluginsPayloadParser {
  @JvmStatic
  fun parse(path: String): List<JvmCompilationTask.Inputs.Plugin> {
    val json = Files.readString(Paths.get(path), StandardCharsets.UTF_8)
    val inputs = JvmCompilationTask.Inputs.newBuilder()
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(json, inputs)
    } catch (e: InvalidProtocolBufferException) {
      throw IllegalArgumentException("invalid plugins payload JSON at $path: ${e.message}", e)
    }
    return inputs.pluginsList
  }
}
