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
package io.bazel.kotlin.builder.mode.jvm.actions

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.model.KotlinModel
import java.nio.file.Path
import java.nio.file.Paths

@ImplementedBy(DefaultOutputJarCreator::class)
interface OutputJarCreator {
    fun createOutputJar(command: KotlinModel.BuilderCommand)
}

private class DefaultOutputJarCreator @Inject constructor(
    val toolInvoker: KotlinToolchain.JarToolInvoker
) : OutputJarCreator {
    private fun MutableList<String>.addAllFrom(dir: Path) = addAll(arrayOf("-C", dir.toString(), "."))
    override fun createOutputJar(command: KotlinModel.BuilderCommand) {
        mutableListOf(
            "cf", command.outputs.jar
        ).also { args ->
            args.addAllFrom(Paths.get(command.directories.classes))
            args.addAllFrom(Paths.get(command.directories.generatedClasses))
        }.let { toolInvoker.invoke(it) }
    }
}
