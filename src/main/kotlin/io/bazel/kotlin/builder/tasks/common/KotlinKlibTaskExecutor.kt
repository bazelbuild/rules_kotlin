package io.bazel.kotlin.builder.tasks.common

import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.model.KlibCompilationTask
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

@Singleton
class KotlinKlibTaskExecutor
@Inject
constructor(
  private val invoker: KotlinToolchain.K2NativeCompilerInvoker,
) {
  private val fileSystem: FileSystem = FileSystems.getDefault()

  fun execute(
    context: CompilationTaskContext,
    task: KlibCompilationTask,
  ) {
    task.compile(context)
  }

  private fun KlibCompilationTask.workingDirectory(): Path = fileSystem.getPath(directories.temp)

  private fun KlibCompilationTask.commonArgs(): MutableList<String> {
    val workDir = workingDirectory()
    if (!workDir.exists()) {
      workDir.toFile().mkdirs()
    }

    val execRoot = fileSystem.getPath(".").absolute()
    return mutableListOf<String>().apply {
      addAll(passThroughFlagsList)
      addAll(
        "-library",
        inputs.librariesList
          .map {
            execRoot
              .resolve(
                it,
              ).absolutePathString()
          }.joinToString(":"),
      )
      addAll("-module-name=${info.moduleName}")
      addAll(inputs.kotlinSourcesList.map { execRoot.resolve(it).absolutePathString() })
    }
  }

  private fun KlibCompilationTask.compile(context: CompilationTaskContext) {
    val args = commonArgs()
    val klibOut = fileSystem.getPath(outputs.klib)
    args.addAll("-produce", "library")
    context.whenTracing { printLines("klib compile args", args) }

    val outputDirectory = klibOut.parent
    val workDir = workingDirectory()

    context.executeCompilerTask(args, invoker::compile)
    context.whenTracing {
      printLines(
        "outputs",
        Files.walk(outputDirectory).map { p -> p.toString() }.collect(Collectors.toList()),
      )
    }
    Files.copy(workDir.resolve(klibOut.fileName), klibOut)
  }

}
