package io.bazel.kotlin.generate

import io.bazel.kotlin.generate.WriteKotlincCapabilities.KotlincCapabilities.Companion.asCapabilities
import io.bazel.kotlin.generate.WriteKotlincCapabilities.KotlincCapability
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.load.java.JvmAbi
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Year
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Generates a list of kotlinc flags from the K2JVMCompilerArguments on the classpath.
 */
object WriteKotlincCapabilities {

  /** Options that are either confusing, useless, or unexpected to be set outside the worker. */
  private val suppressedFlags = setOf(
    "-P",
    "-X",
    "-Xbuild-file",
    "-Xcompiler-plugin",
    "-Xcompiler-plugin",
    "-Xdump-declarations-to",
    "-Xdump-directory",
    "-Xdump-fqname",
    "-Xdump-perf",
    "-Xintellij-plugin-root",
    "-Xplugin",
    "-classpath",
    "-d",
    "-expression",
    "-help",
    "-include-runtime",
    "-jdk-home",
    "-kotlin-home",
    "-module-name",
    "-no-jdk",
    "-no-stdlib",
    "-script",
    "-script-templates",
  )

  @JvmStatic
  fun main(vararg args: String) {
    // TODO: Replace with a real option parser
    val options = args.asSequence()
      .flatMap { t -> t.split("=", limit = 1) }
      .chunked(2)
      .fold(mutableMapOf<String, MutableList<String>>()) { m, (key, value) ->
        m.apply {
          computeIfAbsent(key) { mutableListOf() }.add(value)
        }
      }

    val instance = K2JVMCompilerArguments()

    val capabilitiesName = LanguageVersion.LATEST_STABLE.run {
      "capabilities_${major}.${minor}.bzl.com_github_jetbrains_kotlin.bazel"
    }

    val envPattern = Regex("\\$\\{(\\w+)}")
    val capabilitiesDirectory = options["--out"]
      ?.first()
      ?.let { env ->
        envPattern.replace(env) {
          System.getenv(it.groups[1]?.value)
        }
      }
      ?: error("--out is required")

    FileSystems.getDefault()
      .getPath("$capabilitiesDirectory/$capabilitiesName")
      .apply {
        if (!parent.exists()) {
          Files.createDirectories(parent)
        }
        writeText(
          getArguments(K2JVMCompilerArguments::class.java)
            .filterNot(KotlincCapability::shouldSuppress)
            .asCapabilities()
            .asCapabilitiesBzl(),
          StandardCharsets.UTF_8
        )
      }
      .let {
        println("Wrote to $it")
      }
  }

  private fun getArguments(klass: Class<*>) : Sequence<KotlincCapability> = sequence {
    val instance = K2JVMCompilerArguments()
    klass.superclass?.let {
      yieldAll(getArguments(it))
    }

    for (field in klass.declaredFields) {
      field.getAnnotation(Argument::class.java)?.let { argument ->
        val getter = klass.getMethod(JvmAbi.getterName(field.name))
        yield(
          KotlincCapability(
            flag = argument.value,
            default = "${  getter.invoke(instance)}",
            type = field.type.toString(),
          )
        )
      }
    }
  }

  private class KotlincCapabilities(capabilities: Iterable<KotlincCapability>) :
    List<KotlincCapability> by capabilities.toList() {

    companion object {
      fun Sequence<KotlincCapability>.asCapabilities() = KotlincCapabilities(sorted().toList())
    }

    fun asCapabilitiesBzl() = HEADER + "\n" + joinToString(
      ",\n    ",
      prefix = "KOTLIN_OPTS = [\n    ",
      postfix = "\n]",
      transform = KotlincCapability::asCapabilityFlag,
    )
  }

  private data class KotlincCapability(
    private val flag: String,
    private val default: String,
    private val type: String,
  ) : Comparable<KotlincCapability> {

    fun shouldSuppress()  = flag in suppressedFlags

    fun asCapabilityFlag() = "\"${flag}\""
    override fun compareTo(other: KotlincCapability): Int = flag.compareTo(other.flag)
  }


  private val HEADER = """
    # Copyright ${Year.now()} The Bazel Authors. All rights reserved.
    #
    # Licensed under the Apache License, Version 2.0 (the "License");
    # you may not use this file except in compliance with the License.
    # You may obtain a copy of the License at
    #
    #    http://www.apache.org/licenses/LICENSE-2.0
    #
    # Unless required by applicable law or agreed to in writing, software
    # distributed under the License is distributed on an "AS IS" BASIS,
    # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    # See the License for the specific language governing permissions and
    # limitations under the License.
  """.trimIndent()
}
