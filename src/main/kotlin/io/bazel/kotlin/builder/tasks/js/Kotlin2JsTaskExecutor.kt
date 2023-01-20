package io.bazel.kotlin.builder.tasks.js

import io.bazel.kotlin.builder.toolchain.CompilationException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import io.bazel.kotlin.builder.utils.resolveTwinVerified
import io.bazel.kotlin.model.JsCompilationTask
import java.io.FileOutputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Kotlin2JsTaskExecutor @Inject constructor(
  private val invoker: KotlinToolchain.K2JSCompilerInvoker,
) {

  private val fileSystem: FileSystem = FileSystems.getDefault()

  fun execute(
    context: CompilationTaskContext,
    task: JsCompilationTask,
  ) {
    task.compile(context)

    val jsPath = fileSystem.getPath(task.outputs.js)
    val jsMetaFile = jsPath.resolveTwinVerified(".meta.js")
    val jsDirectory = Files.createDirectories(
      fileSystem.getPath(task.directories.temp)
        .resolve(jsPath.toFile().nameWithoutExtension),
    )
    task.createJar(
      jsDirectory,
      listOf(jsPath, jsPath.resolveTwinVerified(".js.map"), jsMetaFile),
    )
    // this mutates the jsPath file , so do it after creating the jar.
    appendMetaToPrimary(jsPath, jsMetaFile)
    task.createSourceJar()
  }

  private fun JsCompilationTask.compile(context: CompilationTaskContext) {
    val args = mutableListOf<String>().also {
      it.addAll(passThroughFlagsList)
      it.addAll("-libraries", inputs.librariesList.joinToString(":"))
      it.addAll("-output", outputs.js)
      it.addAll("-Xuse-deprecated-legacy-compiler")
      it.addAll(inputs.kotlinSourcesList)
    }
    context.whenTracing { printLines("js compile args", args) }
    context.executeCompilerTask(args, invoker::compile)
  }

  private fun JsCompilationTask.createSourceJar() {
    try {
      SourceJarCreator(Paths.get(outputs.srcjar), false).also { creator ->
        creator.addSources(inputs.kotlinSourcesList.map { Paths.get(it) }.stream())
      }.execute()
    } catch (ex: Throwable) {
      throw CompilationException("could not create source jar", ex)
    }
  }

  /**
   * Append the meta file to the JS file. This is an accepted pattern, and it allows us to not have to export the
   * meta.js file with the js.
   */
  private fun appendMetaToPrimary(jsPath: Path, jsMetaFile: Path) {
    try {
      FileOutputStream(jsPath.toFile(), true).use { Files.copy(jsMetaFile, it) }
    } catch (ex: Throwable) {
      throw CompilationException("could not normalize js file", ex)
    }
  }

  private fun JsCompilationTask.createJar(jsDirectoryPath: Path, rootEntries: List<Path>) {
    try {
      val outputJarPath = Paths.get(outputs.jar)

      JarCreator(outputJarPath).also { creator ->
        creator.addDirectory(jsDirectoryPath)
        creator.addRootEntries(rootEntries.map { it.toString() })
        creator.execute()
      }
    } catch (ex: Throwable) {
      throw CompilationException("error creating js jar", ex)
    }
  }
}
