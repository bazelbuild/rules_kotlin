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

import io.bazel.kotlin.model.KotlinModel


fun KotlinModel.CompilationTask.expandWithSources(
    command: KotlinModel.CompilationTask,
    sources: Iterator<String>
): KotlinModel.CompilationTask =
    command.updateBuilder { builder ->
        sources.partitionSources(
            { builder.inputsBuilder.addKotlinSources(it) },
            { builder.inputsBuilder.addJavaSources(it) })
    }


fun KotlinModel.CompilationTask.expandWithGeneratedSources(
    command: KotlinModel.CompilationTask,
    sources: Iterator<String>
): KotlinModel.CompilationTask =
    command.updateBuilder { builder ->
        sources.partitionSources(
            { builder.inputsBuilder.addGeneratedKotlinSources(it) },
            { builder.inputsBuilder.addGeneratedJavaSources(it) })
    }

private fun KotlinModel.CompilationTask.updateBuilder(
    init: (KotlinModel.CompilationTask.Builder) -> Unit
): KotlinModel.CompilationTask =
    toBuilder().let {
        init(it)
        it.build()
    }

fun Iterator<String>.partitionSources(kt: (String) -> Unit, java: (String) -> Unit) {
    forEach {
        when {
            it.endsWith(".kt") -> kt(it)
            it.endsWith(".java") -> java(it)
            else -> throw IllegalStateException("invalid source file type $it")
        }
    }
}
