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

import com.google.common.collect.ImmutableList
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.bazel.kotlin.builder.KotlinToolchain.CompilerPlugin
import io.bazel.kotlin.model.KotlinModel
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*

@ImplementedBy(DefaultKotlinCompilerPluginArgsEncoder::class)
interface KotlinCompilerPluginArgsEncoder {
    fun encode(command: KotlinModel.BuilderCommandOrBuilder): List<String>
}

class DefaultKotlinCompilerPluginArgsEncoder @Inject internal constructor(
    @CompilerPlugin.Kapt3
    private val kapt3: CompilerPlugin
) : KotlinCompilerPluginArgsEncoder {
    companion object {
        private fun encodeMap(options: Map<String, String>): String {
            val os = ByteArrayOutputStream()
            val oos = ObjectOutputStream(os)

            oos.writeInt(options.size)
            for ((key, value) in options.entries) {
                oos.writeUTF(key)
                oos.writeUTF(value)
            }

            oos.flush()
            return Base64.getEncoder().encodeToString(os.toByteArray())
        }

        private fun encodeMultiMap(options: Map<String, List<String>>): String {
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
    }
    /**
     * Plugin using the undocumented encoding format for kapt3
     */
    inner class PluginArgs internal constructor() {
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
        fun encode(): ImmutableList<String> =
            ImmutableList.of(
                "-Xplugin=${kapt3.jarPath}",
                "-P", "plugin:${kapt3.id}:configuration=${encodeMultiMap(tally)}"
            )
    }

    override fun encode(
        command: KotlinModel.BuilderCommandOrBuilder
    ): List<String> {
        val javacArgs = mutableMapOf<String, String>(
            "-target" to command.info.toolchainInfo.jvm.jvmTarget
        )

        return command.info.plugins.takeIf { it.annotationProcessorsList.isNotEmpty() }?.let { plugin ->
            PluginArgs().let { arg ->
                arg["sources"] = command.outputs.sourceGenDir.toString()
                arg["classes"] = command.outputs.classDirectory.toString()
                arg["stubs"] = command.outputs.tempDirectory.toString()
                arg["incrementalData"] = command.outputs.tempDirectory.toString()
                arg["javacArguments"] = javacArgs.let(::encodeMap)
                arg["aptMode"] = "stubsAndApt"
                arg["correctErrorTypes"] = "true"
//                arg["verbose"] = "true"

                arg["processors"] = plugin.annotationProcessorsList
                    .filter { it.processorClass.isNotEmpty() }
                    .onEach { it.classpathList.forEach { arg.bindMulti("apclasspath", it) } }
                    .joinToString(",") { it.processorClass }
                arg.encode()
            }
        }?.let { ImmutableList.copyOf(it) } ?: ImmutableList.of()
    }
}