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
package io.bazel.ruleskotlin.workers


enum class Flags(val globalFlag: String, val kotlinFlag: String?, internal val mandatory: Boolean) {
    // flags that line up with the java builder.
    LABEL(JavaBuilderFlags.TARGET_LABEL.flag, null, true),
    OUTPUT_CLASSJAR(JavaBuilderFlags.OUTPUT.flag, null, true),
    SOURCES(JavaBuilderFlags.SOURCES.flag, null, true),
    CLASSPATH(JavaBuilderFlags.CLASSPATH.flag, "-cp", true),

    // flags that could be aligned with the java builder.
    OUTPUT_JDEPS("--output_jdeps", null, true),
    COMPILER_OUTPUT_BASE("--compiler_output_base", null, true),

    // flags for kotlin.
    KOTLIN_API_VERSION("--kotlin_api_version", "-api-version", false),
    KOTLIN_LANGUAGE_VERSION("--kotlin_language_version", "-language-version", false),
    KOTLIN_JVM_TARGET("--kotlin_jvm_target", "-jvm-target", false);

    operator fun get(context: Context): String? = context[this]
}