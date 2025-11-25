/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
import java.nio.file.Paths
import java.util.UUID

fun stripNonKotlinArgument(
  arg: String,
  args: MutableList<String>,
): String? {
  val index = args.indexOf(arg)
  if (index < 0 || index + 1 >= args.size) return null
  return args.removeAt(index + 1).also { args.removeAt(index) }
}

@Suppress("unused")
class BuildToolsAPICompiler {
  @OptIn(ExperimentalBuildToolsApi::class)
  fun exec(
    errStream: java.io.PrintStream,
    vararg args: String,
  ): ExitCode {
    System.setProperty("zip.handler.uses.crc.instead.of.timestamp", "true")

    var mutableArgs = args.toMutableList()

    // Set of vararg arguments into BuildToolsAPICompiler not to be passed to the compiler.
    // Allows keeping the reflection-based invocation consistent with BazelK2KVMCompiler
    val snapshots =
      stripNonKotlinArgument("-snapshot", mutableArgs)
        ?.split(":")
        ?.map { File(it) }
        .orEmpty()

    // A manufactured strategy for delineating running KSP plugins and compiling Kotlin.
    // incrementalDir: Base directory for incremental compilation caches (inside Bazel's output tree)
    // incrementalId: Used to delineate Kotlin tasks within a label (i.e. KSP vs Kotlin vs Kapt)
    val incrementalDir = stripNonKotlinArgument("-incremental_dir", mutableArgs)!!
    val incrementalId = stripNonKotlinArgument("-incremental_id", mutableArgs)!!

    // The incremental directory is roughly analogous to a Gradle transform cache directory. Must be unique per compilation target.
    // It is derived from the output jar path to keep it inside Bazel's output tree. This ensures:
    // - The cache is cleaned with `bazel clean`
    // - Different configurations get separate caches automatically
    // - Paths are deterministic and target-specific
    val incrementalDirectory = Paths.get(incrementalDir).resolve(incrementalId).toFile()

    val kotlinService = CompilationService.loadImplementation(this.javaClass.classLoader!!)
    // The execution configuration. Controls in-process vs daemon execution strategies. Default is in-process.
    val executionConfig = kotlinService.makeCompilerExecutionStrategyConfiguration()
    // The compilation configuration. Controls everything related to incremental compilation.
    val compilationConfig =
      kotlinService.makeJvmCompilationConfiguration().apply {
        useIncrementalCompilation(
          // The working directory is where Kotlin stores incremental data. This must be unique per compilation target.
          workingDirectory = incrementalDirectory,
          // For Bazel, this will always be ToBeCalculated. We don't have a way to get the sources changes from the last build.
          sourcesChanges = SourcesChanges.ToBeCalculated,
          approachParameters =
            ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
              // The classpath snapshots files actual at the moment of compilation.
              // These are tracked by Bazel as inputs from dependencies.
              newClasspathSnapshotFiles = snapshots,
              // The shrunk classpath snapshot, a result of the previous compilation.
              // Could point to a non-existent file. At the successful end of the compilation,
              // the shrunk version of the [newClasspathSnapshotFiles] will be stored at this path.
              // Note: This is stored in the incremental directory and not tracked by Bazel.
              shrunkClasspathSnapshot =
                incrementalDirectory
                  .resolve(
                    "shrunk-classpath-snapshot.bin",
                  ).apply {
                    parentFile?.createDirectory()
                  },
            ),
          options =
            makeClasspathSnapshotBasedIncrementalCompilationConfiguration().apply {
              // NOTE: The following settings are to enable "relocatable" compilation caches. I was never able to
              //       determine how to work with these without resulting in sporadic failures.
              //       Notably, turning these on requires all input files to be passed with absolute, not relative paths.
              // setRootProjectDir(incrementalDir.resolve("_main").toFile())
              // setBuildDir(incrementalDir.resolve("_kotlin_incremental").toFile())
              // useOutputDirs(emptyList())

              // An indicator whether incremental compilation will analyze Java files precisely for better changes detection
              usePreciseJavaTracking(true)

              // Incremental compilation uses the PersistentHashMap of the intellij platform for storing caches.
              // An indicator whether the changes should remain in memory and not being flushed to the disk until we could mark the compilation as successful.
              keepIncrementalCompilationCachesInMemory(true)

              // I don't believe we will ever need to force non-incremental
              forceNonIncrementalMode(false)

              // Classpath snapshots and changes will be tracked by Bazel
              assureNoClasspathSnapshotsChanges(false)
            },
        )
        // useLogger(BasicKotlinLogger(true, "/tmp/kotlin_log/$label.log"))
        useKotlinScriptFilenameExtensions(listOf("kts"))
      }
    val result =
      kotlinService.compileJvm(
        ProjectId.ProjectUUID(UUID.randomUUID()),
        executionConfig,
        compilationConfig,
        emptyList(),
        mutableArgs.toList(),
      )

    // BTAPI returns a different type than K2JVMCompiler (CompilationResult vs ExitCode).
    return when (result) {
      CompilationResult.COMPILATION_SUCCESS -> ExitCode.OK
      CompilationResult.COMPILATION_ERROR -> ExitCode.COMPILATION_ERROR
      CompilationResult.COMPILATION_OOM_ERROR -> ExitCode.OOM_ERROR
      CompilationResult.COMPILER_INTERNAL_ERROR -> ExitCode.INTERNAL_ERROR
    }
  }
}
