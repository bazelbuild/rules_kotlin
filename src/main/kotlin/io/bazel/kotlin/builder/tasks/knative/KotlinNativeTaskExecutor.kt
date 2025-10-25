package io.bazel.kotlin.builder.tasks.knative

import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.builder.utils.addAll
import io.bazel.kotlin.model.KotlinNativeCompilationTask
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Singleton
class KotlinNativeTaskExecutor
  @Inject
  constructor(
    private val invoker: KotlinToolchain.K2NativeCompilerInvoker,
  ) {
    private val fileSystem: FileSystem = FileSystems.getDefault()

    fun execute(
      context: CompilationTaskContext,
      task: KotlinNativeCompilationTask,
    ) {
      task.compile(context)
    }

    private fun KotlinNativeCompilationTask.workingDirectory(): Path =
      fileSystem.getPath(directories.temp)

    private fun KotlinNativeCompilationTask.commonArgs(): MutableList<String> {
      val workDir = workingDirectory()
      if (!workDir.exists()) {
        workDir.toFile().mkdirs()
      }

      val konanHome = this.info.toolchainInfo.native.konanHome
      if (!Paths.get(konanHome).exists()) {
        throw IllegalArgumentException("$konanHome doesn't point to konan.home or doesn't exist.")
      }

      System.setProperty("konan.home", konanHome)

      val autoCacheDirectory = workingDirectory().resolve("native_auto_cache")
      if (!autoCacheDirectory.exists()) {
        autoCacheDirectory.toFile().mkdirs()
      }

      val autoCacheFromDirectory = workingDirectory().resolve("native_auto_from")
      if (!autoCacheFromDirectory.exists()) {
        autoCacheFromDirectory.toFile().mkdirs()
      }

      val execRoot = fileSystem.getPath(".")
      return mutableListOf<String>().apply {
        addAll(passThroughFlagsList)
        add("-Xklib-normalize-absolute-path")
        // Avoid downloading any dependencies during the build
        add("-Xoverride-konan-properties=airplaneMode=true")
        add("-nostdlib")

        // the target for which we should compile libaries/binaries to
        add("-target=${info.toolchainInfo.native.kotlinNativeTarget}")

        // Map paths in debug symbols to relative paths to execroot
        add("-Xdebug-prefix-map=" + execRoot + "=.")
        // Use relative paths in klibs
        add("-Xklib-relative-path-base=" + execRoot)
        addAll("-module-name=${info.moduleName}")
        addAll(inputs.kotlinSourcesList.map { execRoot.resolve(it).absolutePathString() })
      }
    }

    private fun KotlinNativeCompilationTask.compile(context: CompilationTaskContext) {
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
