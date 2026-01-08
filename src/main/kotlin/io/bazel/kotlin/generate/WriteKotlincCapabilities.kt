package io.bazel.kotlin.generate

import io.bazel.kotlin.generate.WriteKotlincCapabilities.KotlincCapabilities.Companion.asCapabilities
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Year
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.math.max
import kotlin.streams.toList

/**
 * Generates a list of kotlinc flags from the kotlin-compiler-arguments-description artifact.
 * This artifact provides structured metadata about all compiler options including:
 * - Flag name and description
 * - Type information (boolean, string, string list)
 * - Lifecycle information (introduced version, stabilized version, deprecated version)
 */
object WriteKotlincCapabilities {

  @JvmStatic
  fun main(vararg args: String) {
    // TODO: Replace with a real option parser
    val options = args.asSequence()
      .flatMap { t -> t.split("=", limit = 2) }
      .chunked(2)
      .fold(mutableMapOf<String, MutableList<String>>()) { m, (key, value) ->
        m.apply {
          computeIfAbsent(key) { mutableListOf() }.add(value)
        }
      }

    val envPattern = Regex("\\$\\{(\\w+)}")
    val capabilitiesDirectory = options["--out"]
      ?.first()
      ?.let { env ->
        envPattern.replace(env) {
          System.getenv(it.groups[1]?.value)
        }
      }
      ?.run(FileSystems.getDefault()::getPath)
      ?.apply {
        if (!parent.exists()) {
          Files.createDirectories(parent)
        }
      }
      ?: error("--out is required")

    // Use kotlin-compiler-arguments-description artifact for compiler options metadata
    val capabilities = getArgumentsFromDescription()
      .filterNot(KotlincCapability::shouldSuppress)
      .asCapabilities()

    capabilitiesDirectory.resolve(capabilitiesName).writeText(
      capabilities.asCapabilitiesBzl().toString(),
      StandardCharsets.UTF_8,
    )

    // Generate opts file with full Starlark option structures
    capabilitiesDirectory.resolve(generatedOptsName).writeText(
      capabilities.asGeneratedOptsBzl(),
      StandardCharsets.UTF_8,
    )

    // Generate templates.bzl with both capabilities and generated_opts templates
    capabilitiesDirectory.resolve("templates.bzl").writeText(
      BzlDoc {
        assignment(
          "TEMPLATES",
          list(
            *Files.list(capabilitiesDirectory)
              .filter { it.fileName.toString().startsWith("capabilities_") }
              .map { "Label(${it.fileName.bzlQuote()})" }
              .sorted()
              .toList()
              .toTypedArray(),
          ),
        )
        assignment(
          "GENERATED_OPTS_TEMPLATES",
          list(
            *Files.list(capabilitiesDirectory)
              .filter { it.fileName.toString().startsWith("generated_opts_") }
              .map { "Label(${it.fileName.bzlQuote()})" }
              .sorted()
              .toList()
              .toTypedArray(),
          ),
        )
      }.toString(),
    )
  }

  /** Options that are either confusing, useless, or unexpected to be set outside the worker. */
  private val suppressedFlags = setOf(
    "-P",
    "-X",
    "-Xbuild-file",
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
    // Flags that should be controlled by rules_kotlin, not user configuration
    "-Xfriend-paths",  // Internal module visibility - managed by rules_kotlin
    "-Xjava-source-roots",  // Managed by rules_kotlin based on deps
    "-Xjavac-arguments",  // Use kt_javac_options instead
  )

  /**
   * Known enumerated values for flags that have restricted choices.
   * These values are extracted from kotlinc documentation and used to
   * validate at analysis time rather than compilation time.
   */
  private val enumeratedFlagValues = mapOf(
    "-jvm-default" to listOf("enable", "no-compatibility", "disable"),
    "-Xassertions" to listOf("always-enable", "always-disable", "jvm", "legacy"),
    "-Xlambdas" to listOf("class", "indy"),
    "-Xsam-conversions" to listOf("class", "indy"),
    "-Xstring-concat" to listOf("indy-with-constants", "indy", "inline"),
    "-Xwhen-expressions" to listOf("indy", "inline"),
    "-Xserialize-ir" to listOf("none", "inline", "all"),
    "-Xabi-stability" to listOf("stable", "unstable"),
    "-Xjspecify-annotations" to listOf("ignore", "strict", "warn"),
    "-Xsupport-compatqual-checker-framework-annotations" to listOf("enable", "disable"),
    // Deprecated flag - include for backward compatibility
    "-Xjvm-default" to listOf("disable", "all-compatibility", "all"),
  )

  fun String.increment() = "$this  "
  fun String.decrement() = substring(0, (length - 2).coerceAtLeast(0))

  val capabilitiesName: String by lazy {
    LanguageVersion.LATEST_STABLE.run {
      "capabilities_${major}.${minor}.bzl.com_github_jetbrains_kotlin.bazel"
    }
  }

  val generatedOptsName: String by lazy {
    LanguageVersion.LATEST_STABLE.run {
      "generated_opts_${major}.${minor}.bzl.com_github_jetbrains_kotlin.bazel"
    }
  }

  private class BzlDoc {
    private val HEADER = Comment(
      """
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
    
    # DO NOT EDIT: generated by bazel run //src/main/kotlin/io/bazel/kotlin/generate:kotlin_release_options
  """.trimIndent(),
    )

    val contents: MutableList<Block> = mutableListOf()

    constructor(statements: BzlDoc.() -> Unit) {
      statement(HEADER)
      apply(statements)
    }

    fun statement(vararg statements: Block) {
      contents.addAll(statements)
    }

    class Indent(val spaces: Int = 0) {
      fun increment() = Indent(spaces+2)
      fun decrement() = Indent(max(spaces - 2, 0))
      override fun toString() = " ".repeat(spaces)
      operator fun plus(s:String?) = toString() + s
    }

    fun interface Block {
      fun asString(indent: Indent): String?
      fun asString() = asString(Indent())
    }

    fun interface ValueBlock : Block {
      fun asString(indent: Indent, map: (String) -> String): String?
      override fun asString(indent: Indent) = asString(indent.increment()) { it }
    }

    class Comment(val contents: String) : Block {
      override fun asString(indent: Indent): String? = indent + contents
    }

    override fun toString() = contents.mapNotNull { it.asString() }.joinToString("\n")

    fun assignment(key: String, value: ValueBlock) {
      statement(
        Block { indent ->
          indent + value.asString(indent.increment()) { "$key = $it" }
        },
      )
    }

    fun struct(vararg properties: Pair<String, String?>) = ValueBlock { indent, format ->
      properties
        .mapNotNull { (key, value) ->
          value?.let { "$indent$key = $it" }
        }
        .joinToString(",\n", prefix = "struct(\n", postfix = "\n${indent.decrement()})")
        .run(format)
    }

    fun dict(vararg properties: Pair<String, ValueBlock>) = ValueBlock { indent, format ->
      properties
        .mapNotNull { (key, value) ->
          value.asString(indent.increment())
            ?.let { "$indent${key.bzlQuote()} : $it" }
        }
        .joinToString(",\n", prefix = "{\n", postfix = "\n${indent.decrement()}}")
        .run(format)
    }

    fun list(vararg items: String) = ValueBlock { indent, format ->
      items
        .joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n${indent.decrement()}]",
        ) { "$indent$it" }
        .run(format)
    }
  }

  /**
   * Extract compiler arguments from kotlin-compiler-arguments-description artifact.
   * This collects arguments from all levels up to and including JVM arguments.
   */
  private fun getArgumentsFromDescription(): Sequence<KotlincCapability> = sequence {
    // Collect all arguments for JVM compilation (includes common + JVM specific)
    val jvmLevel = findLevel(kotlinCompilerArguments.topLevel, CompilerArgumentsLevelNames.jvmCompilerArguments)
    if (jvmLevel != null) {
      yieldAll(collectArgumentsFromLevel(jvmLevel))
    }
  }

  /**
   * Find a specific level in the compiler arguments hierarchy.
   */
  private fun findLevel(level: KotlinCompilerArgumentsLevel, targetName: String): KotlinCompilerArgumentsLevel? {
    if (level.name == targetName) return level
    for (subLevel in level.nestedLevels) {
      findLevel(subLevel, targetName)?.let { return it }
    }
    return null
  }

  /**
   * Collect all arguments from a level and its parent chain (merged arguments).
   */
  private fun collectArgumentsFromLevel(level: KotlinCompilerArgumentsLevel): Sequence<KotlincCapability> = sequence {
    // Get arguments from this level and all merged levels
    for (arg in level.arguments) {
      yield(arg.toCapability())
    }
  }

  /**
   * Convert a KotlinCompilerArgument to our KotlincCapability.
   */
  private fun KotlinCompilerArgument.toCapability(): KotlincCapability {
    val defaultValue: String? = when (val vt = valueType) {
      is BooleanType -> vt.defaultValue.current?.let { if (it) "true" else "false" }
      is StringType -> vt.defaultValue.current
      is StringArrayType -> vt.defaultValue.current?.joinToString(",")
      is IntType -> vt.defaultValue.current?.toString()
      else -> null
    }

    val starlarkType = when (valueType) {
      is BooleanType -> StarlarkType.Bool()
      is StringType -> StarlarkType.Str()
      is StringArrayType -> StarlarkType.StrList()
      is IntType -> StarlarkType.Str() // Int represented as string in Starlark
      else -> StarlarkType.Str()
    }

    return KotlincCapability(
      flag = "-$name",
      doc = description.current,
      default = defaultValue,
      type = starlarkType,
      introducedVersion = releaseVersionsMetadata.introducedVersion.toString(),
      stabilizedVersion = releaseVersionsMetadata.stabilizedVersion?.toString(),
    )
  }

  private class KotlincCapabilities(val capabilities: Iterable<KotlincCapability>) {

    companion object {
      fun Sequence<KotlincCapability>.asCapabilities() = KotlincCapabilities(sorted().toList())
    }

    fun asCapabilitiesBzl() = BzlDoc {
      assignment(
        "KOTLIN_OPTS",
        dict(
          *capabilities.map { capability ->
            capability.flag to struct(
              "flag" to capability.flag.bzlQuote(),
              "doc" to capability.doc.bzlQuote(),
              "default" to capability.defaultStarlarkValue(),
              "introduced" to capability.introducedVersion?.bzlQuote(),
              "stabilized" to capability.stabilizedVersion?.bzlQuote(),
            )
          }.toTypedArray(),
        ),
      )
    }

    /**
     * Generate the full _KOPTS dict for opts.kotlinc.bzl.
     */
    fun asGeneratedOptsBzl(): String {
      val tq = "\"\"\""  // triple quote for Python docstrings
      val header = """# Copyright ${Year.now()} The Bazel Authors. All rights reserved.
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

# DO NOT EDIT: generated by bazel run //src/main/kotlin/io/bazel/kotlin/generate:kotlin_release_options

def _map_string_flag(flag):
    ${tq}Create a mapper for string flags that passes value as flag=value.$tq
    def mapper(value):
        if not value:
            return None
        return [flag + "=" + value]
    return mapper

def _map_string_list_flag(flag):
    ${tq}Create a mapper for string list flags that passes each value as flag=value.$tq
    def mapper(values):
        if not values:
            return None
        return [flag + "=" + v for v in values]
    return mapper

"""
      val entries = capabilities.joinToString(",\n") { capability ->
        "    ${capability.starlarkAttrName().bzlQuote()}: ${capability.asOptStructString("    ")}"
      }
      return "$header\nGENERATED_KOPTS = {\n$entries\n}\n"
    }
  }

  data class KotlincCapability(
    val flag: String,
    val doc: String,
    private val default: String?,
    val type: StarlarkType,
    val introducedVersion: String? = null,
    val stabilizedVersion: String? = null,
  ) : Comparable<KotlincCapability> {

    fun shouldSuppress() = flag in suppressedFlags

    fun defaultStarlarkValue(): String? = type.convert(default)

    /** Returns true if this option is considered experimental (not yet stabilized). */
    fun isExperimental(): Boolean = stabilizedVersion == null

    override fun compareTo(other: KotlincCapability): Int = flag.compareTo(other.flag)

    /**
     * Convert flag name to Starlark attribute name.
     * e.g., "-Xcontext-receivers" -> "x_context_receivers"
     *       "-java-parameters" -> "java_parameters"
     */
    fun starlarkAttrName(): String {
      return flag
        .removePrefix("-")
        .replace(Regex("^X"), "x_")  // -Xfoo -> x_foo (add underscore after x)
        .replace("-", "_")
        .lowercase()
    }

    /**
     * Generate the struct string for this option in _KOPTS format.
     * String options with enumerated values use the values list for analysis-time validation.
     */
    fun asOptStructString(indent: String): String {
      val enumValues = enumeratedFlagValues[flag]
      return when (type) {
        is StarlarkType.Bool -> """
          struct(
              flag = ${flag.bzlQuote()},
              args = dict(
                  doc = ${doc.bzlQuote()},
              ),
              type = attr.bool,
              value_to_flag = {True: ["$flag"]},
          )
          """.trimIndent().prependIndent(indent)
        is StarlarkType.Str -> if (enumValues != null) {
          // Enumerated string option - add values list for analysis-time validation
          val valuesStr = enumValues.joinToString(", ") { "\"$it\"" }
          """
          struct(
              flag = ${flag.bzlQuote()},
              args = dict(
                  doc = ${doc.bzlQuote()},
                  values = ["", $valuesStr],
              ),
              type = attr.string,
              value_to_flag = None,
              map_value_to_flag = _map_string_flag(${flag.bzlQuote()}),
          )
          """.trimIndent().prependIndent(indent)
        } else {
          // Free-form string option
          """
          struct(
              flag = ${flag.bzlQuote()},
              args = dict(
                  doc = ${doc.bzlQuote()},
                  default = ${default?.bzlQuote() ?: "\"\""},
              ),
              type = attr.string,
              value_to_flag = None,
              map_value_to_flag = _map_string_flag(${flag.bzlQuote()}),
          )
          """.trimIndent().prependIndent(indent)
        }
        is StarlarkType.StrList -> """
          struct(
              flag = ${flag.bzlQuote()},
              args = dict(
                  doc = ${doc.bzlQuote()},
                  default = [],
              ),
              type = attr.string_list,
              value_to_flag = None,
              map_value_to_flag = _map_string_list_flag(${flag.bzlQuote()}),
          )
          """.trimIndent().prependIndent(indent)
      }
    }
  }

  sealed class StarlarkType(val attr: String) {

    class Bool : StarlarkType("attr.bool") {
      override fun convert(value: String?): String = when (value) {
        "true" -> "True"
        else -> "False"
      }
    }

    class Str : StarlarkType("attr.string") {
      override fun convert(value: String?): String = value?.bzlQuote() ?: "None"
    }

    class StrList : StarlarkType("attr.string_list") {
      override fun convert(value: String?): String =
        value?.let { "default = [${it.bzlQuote()}]" } ?: "[]"
    }

    abstract fun convert(value: String?): String
  }

  private fun Any.bzlQuote(): String {
    var asString = toString()
    val quote = "\"".repeat(if ("\n" in asString || "\"" in asString) 3 else 1)
    return quote + asString + quote
  }
}
