package io.bazel.ruleskotlin.workers.compilers.jvm.utils

import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.PluginDescriptors

class PluginArgBuilder(private val pluginJarPath: String, private val pluginId: String) {
    private val tally = mutableMapOf<String, MutableList<String>>()

    operator fun set(key: String, value: String) {
        check(tally[key] == null) { "value allready set" }
        tally[key] = mutableListOf(value)
    }

    fun bindMulti(key: String, value: String) {
        tally[key].also { if(it != null) it.add(value) else this[key] = value }
    }

    val argList: List<String>
        get() = mutableListOf("-Xplugin=$pluginJarPath").also { args ->
            tally.forEach { key, value ->
                value.forEach { args.add("-P"); args.add("plugin:$pluginId:$key=$it") }
            }
        }
}

fun Context.annotationProcessingGeneratedSources(): Sequence<String>? {
    return PluginDescriptors[this]?.let {
        CompileDirectories[this].annotationProcessingSources.toFile().walkTopDown().filter { it.isFile}.map { it.path }
    }
}

fun Context.annotationProcessingGeneratedJavaSources(): Sequence<String>? = annotationProcessingGeneratedSources()?.filter { it.endsWith(".java") }