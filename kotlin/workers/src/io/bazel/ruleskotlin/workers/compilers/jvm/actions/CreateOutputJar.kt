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
package io.bazel.ruleskotlin.workers.compilers.jvm.actions


import io.bazel.ruleskotlin.workers.BuildAction
import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.Flags
import io.bazel.ruleskotlin.workers.KotlinToolchain
import io.bazel.ruleskotlin.workers.compilers.jvm.Metas
import io.bazel.ruleskotlin.workers.utils.executeAndAwaitSuccess

/**
 * Create a jar from the classes.
 */
class CreateOutputJar(toolchain: KotlinToolchain) : BuildAction("create output jar", toolchain) {
    override fun invoke(ctx: Context): Int {
        try {
            executeAndAwaitSuccess(10,
                    toolchain.JAR_TOOL_PATH,
                    "cf", checkNotNull(Flags.OUTPUT_CLASSJAR[ctx]),
                    "-C", Metas.CLASSES_DIRECTORY.mustGet(ctx).toString(),
                    "."
            )
        } catch (e: Exception) {
            throw RuntimeException("unable to create class jar", e)
        }
        return 0
    }
}
