package io.bazel.kotlin.builder.tasks.klib

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
import kotlin.io.path.pathString

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

      val autoCacheDirectory = workingDirectory().resolve("native_auto_cache")
      if (!autoCacheDirectory.exists()) {
        autoCacheDirectory.toFile().mkdirs()
      }

      val autoCacheFromDirectory = workingDirectory().resolve("native_auto_from")
      if (!autoCacheFromDirectory.exists()) {
        autoCacheFromDirectory.toFile().mkdirs()
      }

      val execRoot = fileSystem.getPath(".").absolute()
      return mutableListOf<String>().apply {
        addAll(passThroughFlagsList)
        // kotlin-native klib compilation requires caching and no way to disable, so pass a temp directory
        // within the current working directory to isolate its cache (which is unique to this worker anyway)
        // Ideally we disable caching though and rely only on Bazel
        add("-Xauto-cache-dir=${autoCacheDirectory.absolutePathString()}")
        add("-Xauto-cache-from=${autoCacheFromDirectory.absolutePathString()}")
        add("-Xklib-normalize-absolute-path")
        addAll("-module-name=${info.moduleName}")
        addAll(inputs.kotlinSourcesList.map { execRoot.resolve(it).absolutePathString() })
      }
    }

    private fun KlibCompilationTask.compile(context: CompilationTaskContext) {
      val args = commonArgs()
      val klibOut = fileSystem.getPath(outputs.klib)
      inputs.librariesList.forEach { library ->
        args.addAll("-library=$library")
      }
      args.addAll("-produce", "library")
      args.addAll("-o", klibOut.pathString.substringBeforeLast('.'))
      context.whenTracing { printLines("klib compile args", args) }

      val outputDirectory = klibOut.parent
      Files.createDirectories(outputDirectory)

      context.executeCompilerTask(args, invoker::compile)
      context.whenTracing {
        printLines(
          "outputs",
          Files.walk(outputDirectory).map { p -> p.toString() }.collect(Collectors.toList()),
        )
      }
    }
  }
