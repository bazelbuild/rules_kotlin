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

import java.io.File

class ArgMap(private val map: Map<String, List<String>>) {
    /**
     * Get the mandatory single value from a key
     */
    fun mandatorySingle(key: String): String =
        optionalSingle(key) ?: throw IllegalArgumentException("$key is not optional")

    fun labelDepMap(key: String) = optional(key)?.asSequence()?.windowed(2, 2)?.map { it[0] to it[1] }?.toMap() ?: emptyMap()

    fun optionalSingle(key: String): String? =
        optional(key)?.let {
            when (it.size) {
                0 -> throw IllegalArgumentException("$key did not have a value")
                1 -> it[0]
                else -> throw IllegalArgumentException("$key should have a single value")
            }
        }

    fun mandatory(key: String): List<String> = optional(key) ?: throw IllegalArgumentException("$key is not optional")
    fun optional(key: String): List<String>? = map[key]
}


object ArgMaps {
    @JvmStatic
    fun from(args: List<String>): ArgMap =
        mutableMapOf<String, MutableList<String>>()
            .also { argsToMap(args, it) }
            .let { ArgMap(it) }

    @JvmStatic
    fun from(file: File): ArgMap = from(file.reader().readLines())

    private fun argsToMap(args: List<String>, argMap: MutableMap<String, MutableList<String>>, isFlag: (String) -> Boolean = { it.startsWith("--") }) {
        var currentKey: String = args.first().also { require(isFlag(it)) { "first arg must be a flag" } }
        val currentValue = mutableListOf<String>()
        val mergeCurrent = {
            argMap.computeIfAbsent(currentKey, { mutableListOf() }).addAll(currentValue)
            currentValue.clear()
        }
        args.drop(1).forEach {
            if (it.startsWith("--")) {
                mergeCurrent()
                currentKey = it
            } else {
                currentValue.add(it)
            }
        }.also { mergeCurrent() }
    }
}