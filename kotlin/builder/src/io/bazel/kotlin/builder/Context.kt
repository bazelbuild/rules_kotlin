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
@file:Suppress("UNCHECKED_CAST")

package io.bazel.kotlin.builder

import io.bazel.kotlin.builder.model.Flags
import java.util.stream.Stream

class Context internal constructor(
        val toolchain: KotlinToolchain,
        val flags: Flags

) {
    private val meta = mutableMapOf<Meta<*>, Any>()

    fun apply(vararg consumers: (Context) -> Unit) {
        Stream.of(*consumers).forEach { it(this) }
    }

    operator fun <T : Any> get(key: Meta<T>): T? = meta[key] as T?
    internal fun <T : Any> putIfAbsent(key: Meta<T>, value: T): T? = meta.putIfAbsent(key, value as Any) as T?
}
