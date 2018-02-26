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

import io.bazel.kotlin.builder.MandatoryMeta
import java.nio.file.Files
import java.nio.file.Path

sealed class CompileState

/**
 * Temporary output directories used durng compilation.
 */
class CompileDirectories(private val outputBase: Path) : CompileState() {
    val classes by lazy { dir("_classes", create = true) }

    /**
     *  The generated sources that need to be compiled and included in the output jar.
     */
    val annotationProcessingSources by lazy { dir("_ap_sources") }
    /**
     * Files other than sources that should be directly bundled into the output jar, this could be resources.
     */
    val annotionProcessingClasses by lazy { dir("_ap_classes") }
    val annotationProcessingStubs by lazy { dir("_ap_stubs") }
    /**
     * The classes generated in here are needed for javac to compile the generated sources.
     */
    val annotationProcessingIncrementalData by lazy { dir("_ap_incremental_data") }

    private fun dir(component: String, create: Boolean = false): Path = outputBase.resolve(component).also {
        if(create) {
            Files.createDirectories(it)
        }
    }

    companion object: MandatoryMeta<CompileDirectories>("compile_directories")
}

class CompilePluginConfig(
        val hasAnnotationProcessors: Boolean = false,
        val args: Array<String> = emptyArray()
): CompileState() {
    companion object: MandatoryMeta<CompilePluginConfig>("plugin_config", defaultValue = CompilePluginConfig())
}