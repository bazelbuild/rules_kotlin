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

import io.bazel.kotlin.builder.BuildAction
import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.model.Metas
import io.bazel.kotlin.builder.utils.executeAndAwaitSuccess
import java.nio.file.Paths

/**
 * Unpack files with the srcjar extension into a temp directory.
 */
class UnpackSourceJars(toolchain: KotlinToolchain) : BuildAction("unpack srcjars", toolchain) {
    override fun invoke(ctx: Context): Int {
        if (ctx.flags.sourceJars != null) {
            check(ctx.flags.sourceJars.isNotEmpty())

            val unpackDir = ctx.flags.tempDirPath.value.resolve("_srcjars").toFile()
                    .also {
                        try {
                            it.mkdirs()
                        } catch(ex: Exception) {
                            throw RuntimeException("could not create unpack directory at $it", ex)
                        }
                    }
            ctx.flags.sourceJars.map { Paths.get(it) }.forEach { srcjar ->
                try {
                    mutableListOf(
                            Paths.get(toolchain.JAR_TOOL_PATH).toAbsolutePath().toString(),
                            "xf", srcjar.toAbsolutePath().toString()
                    ).also { executeAndAwaitSuccess(10, unpackDir, it) }
                } catch (e: Exception) {
                    throw RuntimeException("unable to unpack source jar: $srcjar", e)
                }
            }
            unpackDir.walk()
                    .filter { it.name.endsWith(".kt") || it.name.endsWith(".java") }
                    .map { it.toString() }
                    .toList()
                    // bind the sources even if the list is empty. throw an appropriate error if needed in Initialize.
                    .also { Metas.UNPACKED_SOURCES[ctx] = it }
            return 0
        } else {
            return 0
        }
    }
}
