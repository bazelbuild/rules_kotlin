package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.worker.ContextLog.Logging
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import java.nio.file.Path

class Report(val logging: Logging, val renderer: MessageRenderer = MessageRenderer.PLAIN_RELATIVE_PATHS) : MessageCollector {

  private val messages = mutableListOf<ReportMessage>()

  data class ReportMessage(
    val severity: CompilerMessageSeverity,
    val message: String,
    val location: CompilerMessageSourceLocation?
  )

  override fun clear() {
    messages.clear()
  }

  override fun hasErrors(): Boolean {
    return messages.any { it.severity.isError }
  }

  override fun report(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?
  ) {
    System.out.println("$severity $message $location")
    val reportMessage = ReportMessage(severity, message, location)
    messages.add(reportMessage)
    when {
      reportMessage.severity.isError -> {
        logging.error { renderer.render(severity, message, location) }
      }
      reportMessage.severity.isWarning -> {
        logging.warning { renderer.render(severity, message, location) }
      }
      else -> logging.info { renderer.render(severity, message, location) }
    }
  }
}
