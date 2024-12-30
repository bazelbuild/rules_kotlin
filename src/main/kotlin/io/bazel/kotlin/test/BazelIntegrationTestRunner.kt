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
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object BazelIntegrationTestRunner {
  @JvmStatic
  fun main(args: Array<String>) {
    val fs = FileSystems.getDefault()
    val bazel = fs.getPath(System.getenv("BIT_BAZEL_BINARY"))
    val workspace = fs.getPath(System.getenv("BIT_WORKSPACE_DIR"))
    val unpack = fs.getPath(System.getenv("TEST_TMPDIR")).resolve("rules_kotlin")
    val release = BazelRunFiles.resolveVerifiedFromProperty(
      fs,
      "@rules_kotlin...rules_kotlin_release",
    )

    TarArchiveInputStream(
      GZIPInputStream(
        release.inputStream(),
      ),
    ).use { stream ->
      generateSequence(stream::getNextEntry).forEach { entry ->
        val destination = unpack.resolve(entry.name)
        when {
          entry.isDirectory -> destination.createDirectories()
          entry.isFile -> Files.write(
            destination.apply { parent.createDirectories() },
            stream.readBytes(),
          )

          else -> throw NotImplementedError(entry.toString())
        }
      }
    }

    unpack.resolve("MODULE.bazel")

    val version = bazel.run(workspace, "--version").parseVersion()

    val bazelrc = version.resolveBazelRc(workspace)

    listOf(true, false).filter { bzlmod ->
      bzlmod && workspace.hasModule() || !bzlmod && workspace.hasWorkspace()
    }.forEach { bzlmod ->
      println("Starting bzlmod $bzlmod test")
      val overrideFlag = if (bzlmod) "--override_module=rules_kotlin=$unpack" else "--override_repository=rules_kotlin=$unpack"
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "clean",
        "--expunge",
        "--async"
      ).onFailThrow()
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "shutdown",
      ).onFailThrow()
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "info",
        *version.workspaceFlag(bzlmod),
        overrideFlag
      ).onFailThrow()
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "build",
        overrideFlag,
        "//...",
        *version.workspaceFlag(bzlmod)
      ).onFailThrow()
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "query",
        overrideFlag,
        "@rules_kotlin//...",
        *version.workspaceFlag(bzlmod)
      ).onFailThrow()
      bazel.run(
        workspace,
        "--bazelrc=$bazelrc",
        "query",
        *version.workspaceFlag(bzlmod),
        overrideFlag,
        "kind(\".*_test\", \"//...\")",
      ).ok { process ->
        if (process.stdOut.isNotEmpty()) {
          bazel.run(
            workspace,
            "--bazelrc=$bazelrc",
            "test",
            *version.workspaceFlag(bzlmod),
            overrideFlag,
            "--test_output=all",
            "//...",
          ).onFailThrow()
        }
      }
    }
  }

  fun Path.hasModule() = resolve("MODULE").exists() || resolve("MODULE.bazel").exists()
  private fun Path.hasWorkspace() =
    resolve("WORKSPACE").exists() || resolve("WORKSPACE.bazel").exists()

  sealed class Version {
    abstract fun resolveBazelRc(workspace: Path): Path;

    abstract fun workspaceFlag(isBzlMod: Boolean): Array<String>

    class Head : Version() {
      override fun resolveBazelRc(workspace: Path): Path {
        workspace.resolve(".bazelrc.head").takeIf(Path::exists)?.let {
          return it
        }
        workspace.resolve(".bazelrc").takeIf(Path::exists)?.let {
          return it
        }
        return workspace.resolve("/dev/null")
      }

      override fun workspaceFlag(isBzlMod: Boolean): Array<String> = arrayOf("--enable_bzlmod=$isBzlMod", "--enable_workspace=${!isBzlMod}")
    }

    class Known(private val major: Int, private val minor: Int, private val patch: Int) :
      Version() {
      override fun resolveBazelRc(workspace: Path): Path {
        sequence {
          val parts = mutableListOf(major, minor, patch)
          (parts.size downTo 0).forEach { index ->
            yield("." + parts.subList(0, index).joinToString("-"))
          }
        }
          .map { suffix -> workspace.resolve(".bazelrc${suffix}") }
          .find(Path::exists)
          ?.let { bazelrc ->
            return bazelrc
          }
        return workspace.resolve("/dev/null")
      }

      override fun workspaceFlag(isBzlMod: Boolean): Array<String> = if (isBzlMod) {
        arrayOf("--enable_bzlmod=true")
      } else if (major >= 7) {
        arrayOf("--enable_workspace=true", "--enable_bzlmod=false")
      } else {
        arrayOf("--enable_bzlmod=false")
      }
    }
  }

  private val VERSION_REGEX = Regex("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)([^.]*)")

  private fun Result<ProcessResult>.parseVersion(): Version {
    ok { result ->
      result.stdOut.toString(UTF_8).split("\n")
        // first not empty should have the version
        .find(String::isNotEmpty)?.let { line ->
          if ("no_version" in line) {
            return Version.Head()
          }
          VERSION_REGEX.find(line.trim())?.let { result ->
            return Version.Known(
              major = result.groups["major"]?.value?.toInt() ?: 0,
              minor = result.groups["minor"]?.value?.toInt() ?: 0,
              patch = result.groups["patch"]?.value?.toInt() ?: 0,
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

  fun Path.run(inDirectory: Path, vararg args: String): Result<ProcessResult> =
    ProcessBuilder().command(this.toString(), *args).directory(inDirectory.toFile()).start()
      .let { process ->
        println("Running ${args.joinToString(" ")}...")
        val executor = Executors.newCachedThreadPool();
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
          executor.shutdown();
          executor.awaitTermination(1, TimeUnit.SECONDS);
        }
      }

  private fun InputStream.streamTo(out: OutputStream): Callable<ByteArray> {
    return Callable {
      val result = ByteArrayOutputStream();
      BufferedInputStream(this).apply {
        val buffer = ByteArray(4096)
        var read = 0
        do {
          if (Thread.currentThread().isInterrupted) {
            out.flush()
            break
          }
          result.write(buffer, 0, read);
          out.write(buffer, 0, read)
          read = read(buffer)
        } while (read != -1)
      }
      return@Callable result.toByteArray()
    }
  }
}

