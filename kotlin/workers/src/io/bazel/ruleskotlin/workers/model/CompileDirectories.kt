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

import io.bazel.ruleskotlin.workers.MandatoryMeta
import java.nio.file.Files
import java.nio.file.Path

/**
 * Temporary output directories used durng compilation.
 */
class CompileDirectories(private val outputBase: Path) {
    val classes by lazy { dir("_classes", create = true) }

    // HS RFC: Some of the directories are created lazilly -- so this is more than a model object, using lazy creation here is usefull for debugging.
    val annotationProcessingSources by lazy { dir("_ap_sources") }
    val annotionProcessingClasses by lazy { dir("_ap_classes") }
    val annotationProcessingStubs by lazy { dir("_ap_stubs") }
    val annotationProcessingIncrementalData by lazy { dir("_ap_incremental_data") }

    private fun dir(component: String, create: Boolean = false) = outputBase.resolve(component).also {
        if(create) {
            Files.createDirectories(it)
        }
    }.toString()

    companion object: MandatoryMeta<CompileDirectories>
}