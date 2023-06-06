package io.bazel.kotlin.builder.cmd

import io.bazel.kotlin.builder.jobs.kotlinc.KotlinToJvm
import io.bazel.worker.Worker
import kotlin.system.exitProcess

object Build {
  @JvmStatic
  fun main(args: Array<String>) {
    val compile = KotlinToJvm();
    Worker.from(args.toList()) { flags ->
      return@from 1
    }.run(::exitProcess)
  }
}
