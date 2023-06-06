package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.utils.Arguments
import io.bazel.worker.Status
import io.bazel.worker.Work
import io.bazel.worker.WorkerContext

class CompileJvm : Work {
  val compile = KotlinToJvm()
  override fun invoke(ctx: WorkerContext.TaskContext, args: Iterable<String>): Status {
    return ctx.resultOf { task ->
      val flags = JvmFlags(Arguments(args.toList()), task.directory)
      compile.run(
        JobContext.of(task, flags, flags),
        listOf(
//          CompileKotlinForJvm(),
//          CompileWithAssociates(),
//          GenerateJDeps(),
//          GenerateAbi(),
//          GenerateStubs(),
//          CompileWithPlugins(),
        ),
      )
    }.status
  }
}
