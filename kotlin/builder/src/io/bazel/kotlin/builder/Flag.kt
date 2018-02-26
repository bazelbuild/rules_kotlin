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
package io.bazel.kotlin.builder

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

sealed class Flag(val globalFlag: String, val kotlinFlag: String? = null) {
    open operator fun get(context: Context): String? = context[this]

    inline fun <reified T : Any> renderJsonAndBind(override: JsonAdapter<T>? = null): Meta<T> = object : Meta<T> {
        val adapter = override ?: moshi.adapter(T::class.java)
        override val id: String = "flag $globalFlag"
        override val defaultValue: T? = null
        override fun get(ctx: Context): T? = super.get(ctx) ?: this@Flag[ctx]?.let { adapter.fromJson(it) }
    }

    class Optional(globalFlag: String, kotlinFlag: String? = null) : Flag(globalFlag, kotlinFlag)

    class Mandatory(globalFlag: String, kotlinFlag: String? = null) : Flag(globalFlag, kotlinFlag) {
        override fun get(context: Context): String = requireNotNull(super.get(context)) { "mandatory flag $globalFlag not present" }
    }

    companion object {
        @PublishedApi
        internal val moshi = Moshi.Builder().let {
            it.add(KotlinJsonAdapterFactory())
            it.build()
        }
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