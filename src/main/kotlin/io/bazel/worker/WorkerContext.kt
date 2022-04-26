/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.worker

import io.bazel.worker.ContextLog.FileScope
import io.bazel.worker.ContextLog.Granularity
import io.bazel.worker.ContextLog.Granularity.INFO
import io.bazel.worker.ContextLog.LoggingScope
import io.bazel.worker.Status.ERROR
import io.bazel.worker.Status.INTERNAL_ERROR
import io.bazel.worker.Status.SUCCESS
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InterruptedIOException
import java.io.PrintStream
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler

/** WorkerContext encapsulates logging, filesystem, and profiling for a task invocation. */
class WorkerContext private constructor(
  private val name: String = Companion::class.java.canonicalName,
  private val verbose: Granularity = INFO
) : Closeable, LoggingScope by ContextLogger(name, verbose.level, null) {

  companion object {
    fun <T : Any?> run(
      named: String = "worker",
      verbose: Granularity = INFO,
      report: (ContextLog) -> Unit = {},
      work: WorkerContext.() -> T,
    ): T {
      return WorkerContext(verbose = verbose, name = named).run {
        use(work).also {
          report(contents())
        }
      }
    }
  }

  private class ContextLogger(
    val name: String,
    val level: Level,
    val propagateTo: ContextLogger? = null
  ) : LoggingScope {

    private val profiles = mutableListOf<String>()

    private val out by lazy {
      ByteArrayOutputStream()
    }

    private val handler by lazy {
      StreamHandler(out, SimpleFormatter()).also { h ->
        h.level = this.level
      }
    }

    private val logger: Logger by lazy {
      object : Logger(name, null) {}.apply {
        level = level
        propagateTo?.apply { parent = logger }
        addHandler(handler)
      }
    }

    private val sourceName by lazy {
      propagateTo?.name ?: "global"
    }

    override fun info(msg: () -> String) {
      logger.logp(Level.INFO, sourceName, name, msg)
    }

    override fun error(
      t: Throwable,
      msg: () -> String,
    ) {
      logger.logp(Level.SEVERE, sourceName, name, t, msg)
    }

    override fun error(msg: () -> String) {
      logger.logp(Level.SEVERE, sourceName, name, msg)
    }

    override fun debug(msg: () -> String) {
      logger.logp(Level.FINE, sourceName, name, msg)
    }

    override fun warning(msg: () -> String) {
      logger.logp(Level.WARNING, sourceName, name, msg)
    }

    override fun narrowTo(name: String): LoggingScope {
      return ContextLogger(name, level, this)
    }

    override fun contents() = handler.flush().run { ContextLog(out.toByteArray(), profiles) }

    override fun asPrintStream(): PrintStream = PrintStream(out, true)
  }

  class TaskContext internal constructor(
    override val directory: Path,
    logging: LoggingScope
  ) : FileScope, LoggingScope by logging {
    var status = SUCCESS

    fun step(name:String, task: () -> Status) {
      when (status) {
        SUCCESS ->  status =  task()
        ERROR -> debug { "Skipping $name due to previous errors" }
        INTERNAL_ERROR -> error { "Not executing $name due to previous internal errors" }
      }
    }

    /** resultOf a status supplier that includes information collected in the Context. */
    fun resultOf(executeTaskIn: (TaskContext) -> Unit): TaskResult {
      try {
        executeTaskIn(this)
        return TaskResult(
          status,
          contents()
        )
      } catch (t: Throwable) {
        when (t.causes.lastOrNull()) {
          is InterruptedException, is InterruptedIOException -> error(t) { "ERROR: Interrupted" }
          else -> error(t) { "ERROR: unexpected exception" }
        }
        return TaskResult(
          ERROR,
          contents(),
        )
      }
    }

    private val Throwable.causes
      get(): Sequence<Throwable> {
        return cause?.let { c -> sequenceOf(c) + c.causes } ?: emptySequence()
      }
  }

  /** doTask work in a TaskContext. */
  fun doTask(
    name: String,
    task: (sub: TaskContext) -> Unit
  ): TaskResult {
    info { "start task $name" }
    return WorkingDirectoryContext.use {
      TaskContext(dir, logging = narrowTo(name)).resultOf(task)
    }.also {
      info { "end task $name: ${it.status}" }
    }
  }

  override fun close() {
    info { "ending worker context" }
  }
}
