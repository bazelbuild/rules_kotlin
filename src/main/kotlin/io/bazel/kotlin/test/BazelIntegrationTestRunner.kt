package io.bazel.kotlin.test


import io.bazel.kotlin.builder.utils.BazelRunFiles
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
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
    val unpack = Files.createTempDirectory("rules_kotlin")
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

    listOf(true, false)
      .filter { bzlmod ->
        bzlmod && workspace.hasModule() || !bzlmod && workspace.hasWorkspace()
      }
      .forEach { bzlmod ->
        bazel.run(
          workspace,
          "info",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
        ).onFailure { err ->
          throw err
        }
        bazel.run(
          workspace,
          "build",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
          "//...",
        ).onFailure { err ->
          throw err
        }
        bazel.run(
          workspace,
          "query",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
          "kind(\".*_test\", \"//...\")",
        ).onFailure {
            err ->
          throw err
        }.onSuccess { process ->
          if (process.inputStream.readAllBytes().isNotEmpty()) {
            bazel.run(
              workspace,
              "test",
              "--enable_bzlmod=$bzlmod",
              "--override_repository=rules_kotlin=$unpack",
              "//...",
            ).onFailure { err ->
              throw err
            }
          }
        }

      }
  }

  fun Path.hasModule() = resolve("MODULE").exists() || resolve("MODULE.bazel").exists()
  fun Path.hasWorkspace() = resolve("WORKSPACE").exists() || resolve("WORKSPACE.bazel").exists()


  fun Path.run(inDirectory: Path, vararg args: String): Result<Process> = ProcessBuilder()
    .command(this.toString(), *args)
    .directory(inDirectory.toFile())
    .start()
    .let { process ->
      if (process.waitFor() == 0) {
        return Result.success(process)
      }
      return Result.failure(
        AssertionError(
          """
            $this ${args.joinToString(" ")} exited ${process.exitValue()}:
            stdout:
            ${process.inputStream.readAllBytes().toString(UTF_8)}
            stderr:
            ${process.errorStream.readAllBytes().toString(UTF_8)}
          """.trimIndent(),
        ),
      )
    }
}
