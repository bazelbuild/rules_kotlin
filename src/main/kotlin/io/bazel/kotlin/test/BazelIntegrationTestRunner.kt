package io.bazel.kotlin.test


import io.bazel.kotlin.builder.utils.BazelRunFiles
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
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

    listOf(true, false)
      .filter { bzlmod ->
        bzlmod && workspace.hasModule() || !bzlmod && workspace.hasWorkspace()
      }
      .forEach { bzlmod ->
        bazel.run(
          workspace,
          "--bazelrc=$bazelrc",
          "info",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
        ).onFailThrow()
        bazel.run(
          workspace,
          "--bazelrc=$bazelrc",
          "build",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
          "//...",
        ).onFailThrow()
        bazel.run(
          workspace,
          "--bazelrc=$bazelrc",
          "query",
          "--enable_bzlmod=$bzlmod",
          "--override_repository=rules_kotlin=$unpack",
          "kind(\".*_test\", \"//...\")",
        ).ok { process ->
          if (process.inputStream.readAllBytes().isNotEmpty()) {
            bazel.run(
              workspace,
              "--bazelrc=$bazelrc",
              "test",
              "--enable_bzlmod=$bzlmod",
              "--override_repository=rules_kotlin=$unpack",
              "//...",
            ).onFailThrow()
          }
        }
      }
  }

  private fun Path.hasModule() = resolve("MODULE").exists() || resolve("MODULE.bazel").exists()
  private fun Path.hasWorkspace() = resolve("WORKSPACE").exists() || resolve("WORKSPACE.bazel").exists()
  
  sealed class Version{
    abstract fun resolveBazelRc(workspace: Path): Path;
    
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
    }
    
    class Known(private val major: Int, private val minor: Int, private val patch: Int) : Version() {
      override fun resolveBazelRc(workspace: Path): Path {
        workspace.resolve(".bazelrc.${major}-${minor}-${patch}")
          .takeIf(Path::exists)?.let {
            return it
          }
        workspace.resolve(".bazelrc.${major}-${minor}").takeIf(Path::exists)?.let {
          return it
        }
        workspace.resolve(".bazelrc.${major}").takeIf(Path::exists)?.let {
          return it
        }
        workspace.resolve(".bazelrc.${major}").takeIf(Path::exists)?.let {
          return it
        }
        workspace.resolve(".bazelrc").takeIf(Path::exists)?.let {
          return it
        }
        return workspace.resolve("/dev/null")
      }
    }
  }

  private fun Result<Process>.parseVersion(): Version {
    ok { process ->
      process.inputStream.bufferedReader(UTF_8).use { reader ->
        generateSequence(reader::readLine)
          // first not empty should have the version
          .find(String::isNotEmpty)
          ?.let { line ->
            if ("no_version" in line) {
              return Version.Head()
            }
            val parts = line.split(" ")[1].split(".")
            return Version.Known(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
          }
        throw IllegalStateException("Bazel version not available")
      }
    }
  }

  private fun Result<Process>.onFailThrow() = onFailure {
    throw it
  }

  private inline fun <R> Result<Process>.ok(action: (Process) -> R) = fold(
    onSuccess = action,
    onFailure = { err -> throw err },
  )

  fun Path.run(inDirectory: Path, vararg args: String): Result<Process> = ProcessBuilder()
    .command(this.toString(), *args)
    .directory(inDirectory.toFile())
    .start()
    .let { process ->
      if (process.waitFor(800, TimeUnit.SECONDS) && process.exitValue() == 0) {
        return Result.success(process)
      }
      val out = process.inputStream.readAllBytes().toString(UTF_8)
      val err = process.errorStream.readAllBytes().toString(UTF_8)
      process.destroyForcibly()
      return Result.failure(
        AssertionError(
          """
            $this ${args.joinToString(" ")} exited ${process.exitValue()}:
            stdout:
            $out
            stderr:
            $err
          """.trimIndent(),
        ),
      )
    }
}
