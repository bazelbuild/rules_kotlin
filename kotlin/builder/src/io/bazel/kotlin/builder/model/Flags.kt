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
package io.bazel.kotlin.builder.model

import io.bazel.kotlin.builder.*

/**
 * The flags supported by the worker.
 */
class Flags(argMap: ArgMap) {
    val label = argMap.mandatorySingle(JavaBuilderFlags.TARGET_LABEL.flag)
    val outputClassJar = argMap.mandatorySingle(JavaBuilderFlags.OUTPUT.flag)
    val source = argMap.mandatorySingle(JavaBuilderFlags.SOURCES.flag)
    val classpath = argMap.mandatorySingle(JavaBuilderFlags.CLASSPATH.flag)
    val plugins = argMap.optionalFromJson<PluginDescriptors>("--kt-plugins")
    val outputJdeps = argMap.mandatorySingle("--output_jdeps")

    val compilerOutputBase = argMap.mandatorySingle("--compiler_output_base")
    val kotlinApiVersion = argMap.mandatorySingle("--kotlin_api_version")
    val kotlinLanguageVersion = argMap.mandatorySingle("--kotlin_language_version")
    val kotlinJvmTarget = argMap.mandatorySingle("--kotlin_jvm_target")

    /**
     * These flags are passed through to the compiler verbatim, the rules ensure they are safe. These flags are to toggle features or they carry a single value
     * so the string is tokenized by space.
     */
    val kotlinPassthroughFlags = argMap.optionalSingle("--kotlin_passthrough_flags")

    val kotlinModuleName = argMap.optionalSingle("--kotlin_module_name")
}