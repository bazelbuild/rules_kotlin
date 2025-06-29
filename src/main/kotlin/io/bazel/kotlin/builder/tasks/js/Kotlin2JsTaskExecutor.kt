package io.bazel.kotlin.builder.tasks.js

import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.model.JsCompilationTask
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

@Singleton
class Kotlin2JsTaskExecutor
@Inject
constructor(
  private val invoker: KotlinToolchain.K2JSCompilerInvoker,
) {
  private val fileSystem: FileSystem = FileSystems.getDefault()

  fun execute(
    context: CompilationTaskContext,
    task: JsCompilationTask,
  ) {
    task.compile(context)
  }

  private fun JsCompilationTask.compile(context: CompilationTaskContext): Path {
    if(outputs.hasKlib()) {
      return compileToKlib(context)
    } else {
      return compileToJs(context)
    }
  }

  private fun workingDirectory(): Path = fileSystem.getPath(FileSystems.getDefault().getPath("").toAbsolutePath().toString())

  private fun JsCompilationTask.commonArgs(): MutableList<String> {
    val workDir = workingDirectory()
    val execRoot = fileSystem.getPath(".").absolute()
    return mutableListOf<String>().apply {
        addAll(passThroughFlagsList)
        addAll(
          "-libraries",
          inputs.librariesList
            .map {
              execRoot
                .resolve(
                  it,
                ).absolutePathString()
            }.joinToString(":"),
        )
        addAll("-ir-output-dir", workDir.toString())
        addAll("-Xir-module-name=${info.moduleName}")
        addAll(inputs.kotlinSourcesList.map { execRoot.resolve(it).absolutePathString() })
      }
  }

  private fun JsCompilationTask.compileToKlib(context: CompilationTaskContext): Path {
    val args = commonArgs()
    val klibOut = fileSystem.getPath(outputs.js.jsFile)
    args.add("-Xir-produce-klib-file")
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

    return workDir
  }

  private fun JsCompilationTask.compileToJs(context: CompilationTaskContext): Path {
    val args = commonArgs()
    args.add("-Xir-produce-js")
    val jsOut = fileSystem.getPath(outputs.js.jsFile)
    val outputDirectory = jsOut.parent
    val workDir = workingDirectory()
    context.whenTracing { printLines("js compile args", args) }

    context.executeCompilerTask(args, invoker::compile)
    context.whenTracing {
      printLines(
        "outputs",
        Files.walk(outputDirectory).map { p -> p.toString() }.collect(Collectors.toList()),
      )
    }
    Files.copy(workDir.resolve(jsOut.fileName), jsOut)

    return workDir
  }
}
