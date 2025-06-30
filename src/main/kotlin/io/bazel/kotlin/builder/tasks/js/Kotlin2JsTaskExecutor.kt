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
import kotlin.io.path.exists

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

    private fun JsCompilationTask.compile(context: CompilationTaskContext) {
      compileToKlib(context)
      // If producing JS, add a additional compilation step after creating klib
      if (outputs.js != null && !outputs.js.jsFile.isNullOrEmpty()) {
        compileToJs(context)
      }
    }

    private fun JsCompilationTask.workingDirectory(): Path = fileSystem.getPath(directories.temp)

    private fun JsCompilationTask.commonArgs(): MutableList<String> {
      val workDir = workingDirectory()
      if (!workDir.exists()) {
        workDir.toFile().mkdirs()
      }

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
      val klibOut = fileSystem.getPath(outputs.klib)
      args.add("-Xir-produce-klib-file")
      args.addAll("-ir-output-name=${klibOut.toFile().nameWithoutExtension}")
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
      val klibOut = fileSystem.getPath(outputs.klib)
      args.addAll("-Xinclude=${klibOut.toAbsolutePath()}")
      val jsOut = fileSystem.getPath(outputs.js.jsFile)
      val outputDirectory = jsOut.parent
      args.addAll("-ir-output-name=${jsOut.toFile().nameWithoutExtension}")

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
