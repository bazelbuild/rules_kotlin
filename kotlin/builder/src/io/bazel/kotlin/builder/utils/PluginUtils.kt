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

import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain.CompilerPlugin
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*

/**
 * Plugin using the undocumented encoding format for kapt3
 */
class PluginArgs private constructor(private val kapt: CompilerPlugin) {
    private val tally = mutableMapOf<String, MutableList<String>>()

    operator fun set(key: String, value: String) {
        check(tally[key] == null) { "value allready set" }
        tally[key] = mutableListOf(value)
    }

    fun bindMulti(key: String, value: String) {
        tally[key].also { if (it != null) it.add(value) else this[key] = value }
    }

    // "configuration" is an undocumented kapt3 argument. preparing the arguments this way is the only way to get more than one annotation processor class
    // passed to kotlinc.
    fun encode(): List<String> = listOf("-Xplugin=${kapt.jarPath}", "-P", "plugin:${kapt.id}:configuration=${encodePluginOptions(tally)}")

    private fun encodePluginOptions(options: Map<String, List<String>>): String {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(options.size)
        for ((key, values) in options.entries) {
            oos.writeUTF(key)

            oos.writeInt(values.size)
            for (value in values) {
                oos.writeUTF(value)
            }
        }

        oos.flush()
        return Base64.getEncoder().encodeToString(os.toByteArray())
    }

    companion object {
        fun from(ctx: Context): List<String>? =
            ctx.flags.plugins?.takeIf { it.annotationProcessorsList.isNotEmpty() }?.let { plugin ->
                PluginArgs(ctx.toolchain.KAPT_PLUGIN).let { arg ->
                    arg["sources"] = ctx.flags.sourceGenDir.value.toString()
                    arg["classes"] = ctx.flags.classDir.value.toString()
                    arg["stubs"] = ctx.flags.tempDirPath.value.toString()
                    arg["incrementalData"] = ctx.flags.tempDirPath.value.toString()

                    arg["aptMode"] = "stubsAndApt"
                    arg["correctErrorTypes"] = "true"
//                  arg["verbose"] = "true"

                    arg["processors"] = plugin.annotationProcessorsList
                        .filter { it.processorClass.isNotEmpty() }
                        .onEach { it.classpathList.forEach { arg.bindMulti("apclasspath", it) } }
                        .joinToString(",") { it.processorClass }
                    arg.encode()
                }
            }
    }
}

fun Context.annotationProcessingGeneratedSources(): Sequence<String>? =
    if (flags.sourceGenDir.isInitialized()) {
        flags.sourceGenDir.value.toFile().walkTopDown().filter { it.isFile }.map { it.path }
    } else null

fun Context.annotationProcessingGeneratedJavaSources(): Sequence<String>? = annotationProcessingGeneratedSources()?.filter { it.endsWith(".java") }