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


import io.bazel.kotlin.model.JvmCompilationTask
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*

class KotlinCompilerPluginArgsEncoder(
    private val jarPath: String,
    private val pluginId: String
) {
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
        fun encode(): List<String> =
            listOf(
                "-Xplugin=$jarPath",
                "-P", "plugin:$pluginId:configuration=${encodeMultiMap(
                    tally
                )}"
            )
    }

    fun encode(context: CompilationTaskContext, command: JvmCompilationTask): List<String> {
        val javacArgs = mutableMapOf<String, String>(
            "-target" to command.info.toolchainInfo.jvm.jvmTarget
        )
        val d = command.directories
        return command.info.plugins.takeIf { it.annotationProcessorsList.isNotEmpty() }?.let { plugin ->
            PluginArgs().let { arg ->
                arg["sources"] = d.generatedSources.toString()
                arg["classes"] = d.generatedClasses.toString()
                arg["stubs"] = d.temp.toString()
                arg["incrementalData"] = d.temp.toString()
                arg["javacArguments"] = javacArgs.let(Companion::encodeMap)
                arg["aptMode"] = "stubsAndApt"
                arg["correctErrorTypes"] = "true"
                arg["verbose"] = context.isDebug.toString()

                arg["processors"] = plugin.annotationProcessorsList
                    .filter { it.processorClass.isNotEmpty() }
                    .onEach { processor -> processor.classpathList.forEach { arg.bindMulti("apclasspath", it) } }
                    .joinToString(",") { it.processorClass }
                arg.encode()
            }
        } ?: emptyList()
    }
}