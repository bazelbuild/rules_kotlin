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

import io.bazel.kotlin.builder.KotlinToolchain
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import io.bazel.kotlin.model.KotlinModel
import com.google.devtools.build.lib.view.proto.Deps
import com.google.inject.ImplementedBy
import com.google.inject.Inject

@ImplementedBy(DefaultJacocoProcessor::class)
interface JacocoProcessor {
    fun instrument(command: KotlinModel.BuilderCommand)
}

class DefaultJacocoProcessor @Inject constructor(
    val compiler: KotlinToolchain.KotlincInvoker
) : JacocoProcessor {
    override fun instrument(command: KotlinModel.BuilderCommand) {
        val classDir = Paths.get(command.directories.classes)
        val instr = Instrumenter(OfflineInstrumentationAccessGenerator())

        // Runs Jacoco instrumentation processor over all .class files.
        Files.walkFileTree(
            classDir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!file.fileName.toString().endsWith(".class")) {
                        return FileVisitResult.CONTINUE
                    }

                    val uninstrumentedCopy = Paths.get(file.toString() + ".uninstrumented")
                    Files.move(file, uninstrumentedCopy)
                    BufferedInputStream(Files.newInputStream(uninstrumentedCopy)).use { input ->
                        BufferedOutputStream(Files.newOutputStream(file)).use { output ->
                            instr.instrument(input, output, file.toString())
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
    }
}
