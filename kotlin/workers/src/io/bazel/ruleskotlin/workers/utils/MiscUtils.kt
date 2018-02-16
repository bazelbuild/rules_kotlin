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

package io.bazel.ruleskotlin.workers.utils

import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.model.Flags
import io.bazel.ruleskotlin.workers.model.Metas

fun <T, C: MutableCollection<T>> C.addAll(vararg entries: T): C = this.also { addAll(entries) }

fun String?.supplyIfNullOrBlank(s: () -> String): String = this?.takeIf { it.isNotBlank() } ?: s()

val Context.moduleName: String
    get() = Flags.KOTLIN_MODULE_NAME[this].supplyIfNullOrBlank { "${Metas.PKG[this].trimStart { it == '/' }.replace('/', '_')}-${Metas.TARGET[this]}" }
