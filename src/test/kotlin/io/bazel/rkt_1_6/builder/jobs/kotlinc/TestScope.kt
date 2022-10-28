package io.bazel.rkt_1_6.builder.jobs.jvm

import io.bazel.worker.ContextLog
import java.nio.file.Path
import java.util.logging.Level

data class TestScope(override val directory: Path) : ContextLog.Logging, ContextLog.FileScope {

  val logs = mutableMapOf<Level, MutableList<String>>()

  override fun debug(msg: () -> String) {
    logs.getOrPut(Level.FINE, ::mutableListOf).add(msg())
  }

  override fun info(msg: () -> String) {
    logs.getOrPut(Level.INFO, ::mutableListOf).add(msg())
  }

  override fun warning(msg: () -> String) {
    logs.getOrPut(Level.WARNING, ::mutableListOf).add(msg())
  }

  override fun error(t: Throwable, msg: () -> String) {
    logs.getOrPut(Level.SEVERE, ::mutableListOf).add(msg() + t)
  }

  override fun error(msg: () -> String) {
    logs.getOrPut(Level.SEVERE, ::mutableListOf).add(msg())
  }
}
