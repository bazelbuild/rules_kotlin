package io.bazel.kotlin.generate

import io.bazel.kotlin.generate.WriteKotlincCapabilities.KotlincCapabilities.Companion.asCapabilities
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.file.FileSystems
import kotlin.io.path.readText
import kotlin.io.path.writeText

object WriteKotlincCapabilities {

  @JvmStatic
  fun main(vararg args: String) {
    val instance = K2JVMCompilerArguments()

    val capabilitiesName = LanguageVersion.LATEST_STABLE.run {
      "capabilities_${major}.${minor}.bzl"
    }

    FileSystems.getDefault().getPath("/tmp/$capabilitiesName").apply {
      writeText(
        K2JVMCompilerArguments::class.members.asSequence()
          .map { member ->
            member.annotations.find { it is Argument }?.let { argument ->
              member to (argument as Argument)
            }
          }
          .filterNotNull()
          .map { (member, argument) ->
            KotlincCapability(
              flag = argument.value,
              default = "${member.call(instance)}",
              type = member.returnType.toString(),
            )
          }.asCapabilities().asCapabilitiesBzl())
    }.let{
      println(it)
      println(it.readText())
    }
  }

  private class KotlincCapabilities(capabilities: Iterable<KotlincCapability>) :
    List<KotlincCapability> by capabilities.toList() {

    companion object {
      fun Sequence<KotlincCapability>.asCapabilities() = KotlincCapabilities(toList())
    }

    fun asCapabilitiesBzl() = joinToString(",", prefix = "[", postfix = "]") { "\"${it.flag}\"" }
  }

  private data class KotlincCapability(
    val flag: String,
    val default: String,
    val type: String,
  ) {
    fun asCapabilityFlag() = flag
  }
}
