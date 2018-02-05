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
package io.bazel.ruleskotlin.workers

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

sealed class Flag(val globalFlag: String, val kotlinFlag: String? = null) {
    open operator fun get(context: Context): String? = context[this]

    class Optional(globalFlag: String, kotlinFlag: String? = null) : Flag(globalFlag, kotlinFlag)

    class Mandatory(globalFlag: String, kotlinFlag: String? = null) : Flag(globalFlag, kotlinFlag) {
        override fun get(context: Context): String = requireNotNull(super.get(context)) { "mandatory flag $globalFlag not present" }
    }
}

/**
 * all of the static flag properties declared in a class.
 */
// works for objects only.
private fun <T : Any> KClass<T>.allFlags(): Sequence<Flag> {
    val obj = requireNotNull(this.objectInstance) { "only collects flag instances from classes with an object instance" }
    return declaredMemberProperties.asSequence().mapNotNull {
        it.get(obj).takeIf(Flag::class::isInstance).let { it as Flag }
    }
}

typealias FlagNameMap = Map<String, Flag>

/**
 * Map from flag name to flag collected from the static properteis declared in a class.
 */
fun <T : Any> KClass<T>.flagsByName(): FlagNameMap = allFlags().associateBy { it.globalFlag }