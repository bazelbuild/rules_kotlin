package io.bazel.kotlin.builder.cmd

import io.bazel.worker.Worker
import kotlin.system.exitProcess

object Build {
  @JvmStatic
  fun main(args: Array<String>) {
    Worker.from(args.toList()) {
      return@from 1
    }.run(::exitProcess)
  }
}
