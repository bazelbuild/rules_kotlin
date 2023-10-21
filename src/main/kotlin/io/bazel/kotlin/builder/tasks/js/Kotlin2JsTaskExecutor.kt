package io.bazel.kotlin.builder.tasks.js

import io.bazel.kotlin.builder.toolchain.CompilationException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
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
import kotlin.io.path.nameWithoutExtension

@Singleton
class Kotlin2JsTaskExecutor @Inject constructor(
  private val invoker: KotlinToolchain.K2JSCompilerInvoker,
) {

  private val fileSystem: FileSystem = FileSystems.getDefault()

  fun execute(
    context: CompilationTaskContext,
    task: JsCompilationTask,
  ) {
    val outputDirectory = task.compile(context)
    task.createJar(outputDirectory)
    task.createSourceJar()
  }

  private fun JsCompilationTask.compile(context: CompilationTaskContext): Path {
    val jsOut = fileSystem.getPath(outputs.js)
    val outputDirectory = jsOut.parent
    val baseName = jsOut.fileName.nameWithoutExtension
    val mapOut = outputDirectory.resolve("$baseName.js.map")
    val workingDirectory = fileSystem.getPath(directories.temp)

    val execRoot = fileSystem.getPath(".").absolute()

    val args = mutableListOf<String>().apply {
      addAll(passThroughFlagsList)
      add("-Xdisable-default-scripting-plugin")
      add("-Xir-produce-js")
      add("-progressive")
      add("-Xoptimize-generated-js=false")
      addAll(
        "-libraries",
        inputs.librariesList.map { execRoot.resolve(it).absolutePathString() }.joinToString(":"),
      )
      addAll("-ir-output-name", baseName)
      addAll("-ir-output-dir", workingDirectory.toString())
      addAll("-Xir-module-name=${info.moduleName}")
      addAll(inputs.kotlinSourcesList.map { execRoot.resolve(it).absolutePathString() })
    }

    context.whenTracing { printLines("js compile args", args) }
    context.executeCompilerTask(args, invoker::compile)
    context.whenTracing {
      printLines(
        "outputs",
        Files.walk(outputDirectory).map { p -> p.toString() }.collect(Collectors.toList()),
      )
    }
    Files.copy(workingDirectory.resolve(jsOut.fileName), jsOut)
    Files.copy(workingDirectory.resolve(mapOut.fileName), mapOut)

    return workingDirectory
  }

  private fun JsCompilationTask.createSourceJar() {
    try {
      SourceJarCreator(fileSystem.getPath(outputs.srcjar), false).also { creator ->
        creator.addSources(inputs.kotlinSourcesList.map { fileSystem.getPath(it) }.stream())
      }.execute()
    } catch (ex: Throwable) {
      throw CompilationException("could not create source jar", ex)
    }
  }

  private fun JsCompilationTask.createJar(jsDirectoryPath: Path) {
    try {
      JarCreator(fileSystem.getPath(outputs.jar)).use { creator ->
        creator.addDirectory(jsDirectoryPath)
        creator.execute()
      }
    } catch (ex: Throwable) {
      throw CompilationException("error creating js jar", ex)
    }
  }
}
