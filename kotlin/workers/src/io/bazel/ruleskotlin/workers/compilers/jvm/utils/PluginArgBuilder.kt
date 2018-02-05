package io.bazel.ruleskotlin.workers.compilers.jvm.utils

import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.PluginDescriptors
import java.nio.file.Paths

class PluginArgBuilder(private val pluginJarPath: String, private val pluginId: String) {
    private val tally = mutableMapOf<String, String>()

    operator fun set(key: String, value: String) {
        tally[key] = value
    }

    val argList: List<String>
        get() = mutableListOf("-Xplugin=$pluginJarPath").also { args ->
            tally.forEach { args.add("-P"); args.add("plugin:$pluginId:${it.key}=${it.value}") }
        }
}

fun Context.annotationProcessingGeneratedSources(): Sequence<String>? {
    return PluginDescriptors[this]?.let {
        Paths.get(CompileDirectories[this].annotationProcessingSources).toFile().walkTopDown().filter { it.isFile}.map { it.path }
    }
}

fun Context.annotationProcessingGeneratedJavaSources(): Sequence<String>? = annotationProcessingGeneratedSources()?.filter { it.endsWith(".java") }