package io.bazel.kotlin.builder.tasks.klib

import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.model.KlibCompilationTask
import java.nio.file.*
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

      val konanHome = this.info.toolchainInfo.native.konanHome
      if(!Paths.get(konanHome).exists()) {
        throw IllegalArgumentException("$konanHome doesn't point to konan.home or doesn't exist.")
      }

      System.setProperty("konan.home", konanHome);

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
        add("-Xklib-normalize-absolute-path")
        // Avoid downloading any dependencies during the build
        add("-Xoverride-konan-properties=airplaneMode=true")
        add("-nostdlib")
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
