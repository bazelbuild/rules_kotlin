package io.bazel.kotlin.generate

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

object ReadK2JVMOptions {

  @JvmStatic
  fun main(vararg args: String) {
    val instance = K2JVMCompilerArguments()

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
          type = member.returnType.toString()
        )
      }.forEach {
        println(it)
      }
  }

  private data class KotlincCapability(
    val flag:String,
    val default:String,
    val type:String
  )
}
