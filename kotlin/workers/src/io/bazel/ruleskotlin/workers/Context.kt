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

package io.bazel.ruleskotlin.workers

import java.util.*
import java.util.stream.Stream

class Context private constructor(args: List<String>) {
    private val args = EnumMap<Flags, String>(Flags::class.java)
    private val meta = HashMap<Meta<*>, Any>()

    init {
        if (args.size % 2 != 0) {
            throw RuntimeException("args should be k,v pairs")
        }

        for (i in 0 until args.size / 2) {
            val flag = args[i * 2]
            val value = args[i * 2 + 1]
            val field = ALL_FIELDS_MAP[flag] ?: throw RuntimeException("unrecognised arg: " + flag)
            this.args[field] = value
        }

        MANDATORY_FIELDS.asSequence()
                .filterNot { this.args.containsKey(it) }
                .forEach { throw RuntimeException("mandatory arg missing: " + it.globalFlag) }
    }

    fun of(vararg fields: Flags): EnumMap<Flags, String> {
        val result = EnumMap<Flags, String>(Flags::class.java)
        for (field in fields) {
            val value = args[field]
            if (value != null) {
                result[field] = value
            }
        }
        return result
    }

    fun apply(vararg consumers: (Context) -> Unit) {
        Stream.of(*consumers).forEach { it(this) }
    }

    internal operator fun get(field: Flags): String? = args[field]
    internal operator fun <T : Any> get(key: Meta<T>): T? = meta[key] as T?
    internal fun <T : Any> putIfAbsent(key: Meta<T>, value: T): T? = meta.putIfAbsent(key, value as Any) as T?

    companion object {
        private val ALL_FIELDS_MAP = Flags.values().map { it.globalFlag to Flags.valueOf(it.name) }.toMap()
        private val MANDATORY_FIELDS = Flags.values().filter { x -> x.mandatory }

        fun from(args: List<String>): Context {
            return Context(args)
        }
    }
}
