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
package io.bazel.ruleskotlin.workers.model

import io.bazel.ruleskotlin.workers.Flag

/**
 * The flags supported by the worker.
 */
object Flags {
    val LABEL = Flag.Mandatory(JavaBuilderFlags.TARGET_LABEL.flag)
    val OUTPUT_CLASSJAR = Flag.Mandatory(JavaBuilderFlags.OUTPUT.flag)
    val SOURCES = Flag.Mandatory(JavaBuilderFlags.SOURCES.flag)
    val CLASSPATH = Flag.Mandatory(JavaBuilderFlags.CLASSPATH.flag, "-cp")

    val PLUGINS = Flag.Optional("--kt-plugins")

    val OUTPUT_JDEPS = Flag.Mandatory("--output_jdeps")
    val COMPILER_OUTPUT_BASE = Flag.Mandatory("--compiler_output_base")

    val KOTLIN_API_VERSION = Flag.Optional("--kotlin_api_version", "-api-version")
    val KOTLIN_LANGUAGE_VERSION = Flag.Optional("--kotlin_language_version", "-language-version")
    val KOTLIN_JVM_TARGET = Flag.Optional("--kotlin_jvm_target", "-jvm-target")
}