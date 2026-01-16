/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.bazel.kotlin.test

import io.bazel.kotlin.builder.utils.BazelRunFiles
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Integration test runner for incremental compilation tests.
 *
 * This runner:
 * 1. Copies the test workspace excluding .new/.delete files
 * 2. Runs initial Bazel build with IC logging enabled
 * 3. Applies modifications (copy .new files, delete .delete files)
 * 4. Runs incremental build
 * 5. Extracts and compares IC logs against expected build.log
 */
object ICIntegrationTestRunner {
  private val ACTION_EXTENSION_REGEX = Regex(""".*\.(new|delete)\d*$""")

  @JvmStatic
  fun main(args: Array<String>) {
    val fs = FileSystems.getDefault()
    val bazel = fs.getPath(System.getenv("BIT_BAZEL_BINARY"))
    val workspace = fs.getPath(System.getenv("BIT_WORKSPACE_DIR"))
    val testTmpDir = fs.getPath(System.getenv("TEST_TMPDIR"))
    val workingCopy = testTmpDir.resolve("workspace")

    // Expected log is in the workspace directory
    val expectedLogPath = workspace.resolve("build.log")
    if (!expectedLogPath.exists()) {
      throw IllegalArgumentException("Missing build.log in workspace: $workspace")
    }

    // Unpack the release tarball
    val unpack = testTmpDir.resolve("rules_kotlin")
    val release = BazelRunFiles.resolveVerifiedFromProperty(
      fs,
      "@rules_kotlin...rules_kotlin_release",
    )
    unpackRelease(release, unpack)

    // Copy workspace excluding .new/.delete files
    copyWorkspace(workspace, workingCopy)

    // Detect bazel version and setup flags
    val version = bazel.run(workingCopy, "--version").parseVersion()
    val (startupFlags, commandFlags) = buildWorkspaceFlags(workingCopy, unpack, version)
    val icFlags = arrayOf("--@rules_kotlin//kotlin/settings:experimental_ic_enable_logging=True")

    println("=== Running initial build ===")
    bazel.run(
      workingCopy,
      *startupFlags,
      "build",
      *commandFlags,
      *icFlags,
      "//...",
    ).onFailThrow()

    // Apply modifications
    println("=== Applying modifications ===")
    applyModifications(workspace, workingCopy, stage = 0)

    // Run incremental build
    println("=== Running incremental build ===")
    val result = bazel.run(
      workingCopy,
      *startupFlags,
      "build",
      *commandFlags,
      *icFlags,
      "//...",
    )

    // Extract IC logs from output
    val actualLog = extractICLog(result)
    val expectedLog = Files.readString(expectedLogPath, UTF_8).trim()

    println("=== Expected IC Log ===")
    println(expectedLog)
    println("=== Actual IC Log ===")
    println(actualLog)

    // Compare logs
    if (normalizeLog(actualLog) != normalizeLog(expectedLog)) {
      throw AssertionError(
        """
        IC log mismatch!

        Expected:
        $expectedLog

        Actual:
        $actualLog
        """.trimIndent(),
      )
    }

    println("=== IC Integration Test PASSED ===")
  }

  private fun unpackRelease(release: Path, destination: Path) {
    TarArchiveInputStream(
      GZIPInputStream(
        release.inputStream(),
      ),
    ).use { stream ->
      generateSequence(stream::getNextEntry).forEach { entry ->
        val dest = destination.resolve(entry.name)
        when {
          entry.isDirectory -> dest.createDirectories()
          entry.isFile -> Files.write(
            dest.apply { parent.createDirectories() },
            stream.readBytes(),
          )
          else -> throw NotImplementedError(entry.toString())
        }
      }
    }
  }

  private fun copyWorkspace(src: Path, dst: Path) {
    Files.walk(src).forEach { path ->
      var relativePath = src.relativize(path).toString()
      // Skip .new, .delete, and build.log files (build.log is only for comparison)
      if (!relativePath.matches(ACTION_EXTENSION_REGEX) && !relativePath.endsWith("build.log")) {
        // Rename BUILD.bazel.txt to BUILD.bazel (to avoid subpackage issues in the source tree)
        if (relativePath.endsWith("BUILD.bazel.txt")) {
          relativePath = relativePath.removeSuffix(".txt")
        }
        val target = dst.resolve(relativePath)
        if (path.isDirectory()) {
          target.createDirectories()
        } else {
          target.parent.createDirectories()
          Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }
  }

  private fun applyModifications(testData: Path, workingCopy: Path, stage: Int) {
    val deleteSuffix = if (stage > 0) ".delete$stage" else ".delete"
    val newSuffix = if (stage > 0) ".new$stage" else ".new"

    Files.walk(testData).forEach { path ->
      val name = path.name
      when {
        name.endsWith(newSuffix) -> {
          // Copy .new file to replace original
          val targetName = name.removeSuffix(newSuffix)
          val target = workingCopy.resolve(testData.relativize(path.parent)).resolve(targetName)
          println("  Updating: ${testData.relativize(path.parent).resolve(targetName)}")
          Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
        }
        name.endsWith(deleteSuffix) -> {
          // Delete original file
          val targetName = name.removeSuffix(deleteSuffix)
          val target = workingCopy.resolve(testData.relativize(path.parent)).resolve(targetName)
          println("  Deleting: ${testData.relativize(path.parent).resolve(targetName)}")
          Files.deleteIfExists(target)
        }
      }
    }
  }

  private fun extractICLog(result: Result<ProcessResult>): String {
    val output = result.fold(
      onSuccess = { "${it.stdOut.toString(UTF_8)}\n${it.stdErr.toString(UTF_8)}" },
      onFailure = { throw it },
    )
    // Only extract the most meaningful IC log lines - compile iteration shows which files were compiled
    return output.lines()
      .filter { it.contains("[IC DEBUG] compile iteration:") }
      .joinToString("\n")
  }

  private fun normalizeLog(log: String): String {
    return log.trim()
      .lines()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .joinToString("\n")
  }

  /**
   * Returns a pair of (startupFlags, commandFlags).
   * Startup flags (like --bazelrc) must come before the command.
   * Command flags (like --enable_bzlmod) come after the command.
   */
  private fun buildWorkspaceFlags(workspace: Path, unpack: Path, version: Version): Pair<Array<String>, Array<String>> {
    val startupFlags = mutableListOf<String>()
    val commandFlags = mutableListOf<String>()

    // Bazelrc is a startup option
    val bazelRc = findBazelRc(workspace, version)
    startupFlags.add("--bazelrc=$bazelRc")

    // bzlmod/workspace handling - these are command flags
    if (workspace.hasModule()) {
      commandFlags.add("--enable_bzlmod=true")
      commandFlags.add("--override_module=rules_kotlin=$unpack")
      if (version >= Version.Known(7, 0, 0)) {
        commandFlags.add("--enable_workspace=false")
      }
    } else if (workspace.hasWorkspace()) {
      commandFlags.add("--override_repository=rules_kotlin=$unpack")
      commandFlags.add("--enable_bzlmod=false")
      if (version >= Version.Known(7, 0, 0)) {
        commandFlags.add("--enable_workspace=true")
      }
    }

    return Pair(startupFlags.toTypedArray(), commandFlags.toTypedArray())
  }

  private fun findBazelRc(workspace: Path, version: Version): String {
    return when (version) {
      is Version.Head -> {
        sequenceOf(".bazelrc.head", ".bazelrc")
          .map(workspace::resolve)
          .firstOrNull(Path::exists)
          ?.toString()
          ?: "/dev/null"
      }
      is Version.Known -> {
        val parts = listOf(version.major, version.minor, version.patch)
        (parts.size downTo 0).asSequence()
          .map { idx -> "." + parts.subList(0, idx).joinToString("-") }
          .map { suffix -> workspace.resolve(".bazelrc$suffix") }
          .firstOrNull(Path::exists)
          ?.toString()
          ?: "/dev/null"
      }
    }
  }

  private fun Path.hasModule() = resolve("MODULE").exists() || resolve("MODULE.bazel").exists()
  private fun Path.hasWorkspace() =
    resolve("WORKSPACE").exists() || resolve("WORKSPACE.bazel").exists()

  sealed class Version : Comparable<Version> {
    override fun compareTo(other: Version): Int = 1

    class Head : Version() {
      override fun compareTo(other: Version): Int = (other as? Head)?.let { 0 } ?: 1
    }

    class Known(val major: Int, val minor: Int, val patch: Int) : Version() {
      override fun compareTo(other: Version): Int {
        return (other as? Known)?.let {
          when {
            other.major > major -> -1
            other.major < major -> 1
            other.minor > minor -> -1
            other.minor < minor -> 1
            other.patch > patch -> -1
            other.patch < patch -> 1
            else -> 0
          }
        } ?: -1
      }
    }
  }

  private val VERSION_REGEX = Regex("""(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)([^.]*)""")

  private fun Result<ProcessResult>.parseVersion(): Version {
    ok { result ->
      result.stdOut.toString(UTF_8).split("\n")
        .find(String::isNotEmpty)?.let { line ->
          if ("no_version" in line) {
            return Version.Head()
          }
          VERSION_REGEX.find(line.trim())?.let { matchResult ->
            return Version.Known(
              major = matchResult.groups["major"]?.value?.toInt() ?: 0,
              minor = matchResult.groups["minor"]?.value?.toInt() ?: 0,
              patch = matchResult.groups["patch"]?.value?.toInt() ?: 0,
            )
          }
        }
      throw IllegalStateException("Bazel version not available")
    }
  }

  data class ProcessResult(
    val exit: Int,
    val stdOut: ByteArray,
    val stdErr: ByteArray,
  )

  private fun Result<ProcessResult>.onFailThrow() = onFailure {
    throw it
  }

  private inline fun <R> Result<ProcessResult>.ok(action: (ProcessResult) -> R) = fold(
    onSuccess = action,
    onFailure = { err -> throw err },
  )

  private fun Path.run(inDirectory: Path, vararg args: String): Result<ProcessResult> =
    ProcessBuilder().command(this.toString(), *args).directory(inDirectory.toFile()).start()
      .let { process ->
        println("Running [$fileName ${args.joinToString(" ")}]...")
        val executor = Executors.newCachedThreadPool()
        try {
          val stdOut = executor.submit(process.inputStream.streamTo(System.out))
          val stdErr = executor.submit(process.errorStream.streamTo(System.out))
          if (process.waitFor(600, TimeUnit.SECONDS) && process.exitValue() == 0) {
            return Result.success(
              ProcessResult(
                exit = 0,
                stdErr = stdErr.get(),
                stdOut = stdOut.get(),
              ),
            )
          }
          process.destroyForcibly()
          return Result.failure(
            AssertionError(
              """
              $this ${args.joinToString(" ")} exited ${process.waitFor()}:
              stdout:
              ${stdOut.get().toString(UTF_8)}
              stderr:
              ${stdErr.get().toString(UTF_8)}
              """.trimIndent(),
            ),
          )
        } finally {
          executor.shutdown()
          executor.awaitTermination(1, TimeUnit.SECONDS)
        }
      }

  private fun InputStream.streamTo(out: OutputStream): Callable<ByteArray> {
    return Callable {
      val result = ByteArrayOutputStream()
      BufferedInputStream(this).apply {
        val buffer = ByteArray(4096)
        var read = 0
        do {
          if (Thread.currentThread().isInterrupted) {
            out.flush()
            break
          }
          result.write(buffer, 0, read)
          out.write(buffer, 0, read)
          read = read(buffer)
        } while (read != -1)
      }
      return@Callable result.toByteArray()
    }
  }
}
