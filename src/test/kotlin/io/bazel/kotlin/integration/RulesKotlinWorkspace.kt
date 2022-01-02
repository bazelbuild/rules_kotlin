package io.bazel.kotlin.integration

import io.bazel.kotlin.integration.WriteWorkspace.BzlWorkspace
import io.bazel.kotlin.integration.WriteWorkspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.util.concurrent.TimeUnit

class RulesKotlinWorkspace private constructor(
  private val rulesRepo: String,
  private val write: Workspace
) : Workspace by write {

  companion object {
    fun write(
      rulesRepo: String = "rules_kotlin_release",
      contents: RulesKotlinWorkspace.() -> Unit
    ): Path {
      return WriteWorkspace.using<RulesKotlinWorkspace> {
        RulesKotlinWorkspace(rulesRepo, this)
          .apply(contents)
      }
    }

    fun Path.build(vararg targets: String): BuildResult {
      val out = Files.createTempFile("out", "txt").toFile().apply {
        deleteOnExit()
      }
      val err = Files.createTempFile("err", "txt").toFile().apply {
        deleteOnExit()
      }

      return ProcessBuilder()
        .command(
          listOf(
            "bazel",
            "build",
          ) + targets
        )
        .redirectOutput(out)
        .redirectError(err)
        .directory(this.toFile())
        .start().runCatching {
          if (!waitFor(5, TimeUnit.MINUTES)) {
            error("build took too long:\nout: ${out.readText(Charsets.UTF_8)}\nerr: ${err.readText(
              Charsets.UTF_8
            )}")
          }
          BuildResult(
            exit = exitValue(),
            out = out.readText(Charsets.UTF_8),
            err = err.readText(Charsets.UTF_8)
          )
        }
        .recover { exception ->
          BuildResult(
            exit = 1,
            out = "",
            err = exception.toString()
          )
        }
        .getOrThrow()
    }
    data class BuildResult(
      val exit: Int,
      val out: String,
      val err: String,
    )
  }

  override fun workspace(contents: BzlWorkspace.() -> Unit) {
    val rulesRepo = this.rulesRepo
    val (location, archive_repository) = defineArchiveRepository()
    val releaseArchive = writeReleaseArchive().toString()
    write.workspace {
      load(location, archive_repository)
      archive_repository(
        "name" to rulesRepo,
        "path" to releaseArchive
      )
      load("@$rulesRepo//kotlin:repositories.bzl", "kotlin_repositories")
      "kotlin_repositories"()
      apply(contents)
    }
  }

  private fun writeReleaseArchive(): Path {
    return RulesKotlinWorkspace::class.java.classLoader.getResourceAsStream("_release.tgz")
      ?.let { stream ->
        Files.createTempDirectory("rules_kotlin_release").resolve("$rulesRepo.tgz").also {
          Files.newOutputStream(it, CREATE_NEW).buffered().write(stream.readAllBytes())
        }
      }
      ?: error("Cannot find release repo")
  }

  private fun defineArchiveRepository(): Pair<String, String> {
    starlark("archive_repository.bzl") {
      "def _archive_repository_impl"(!"repository_ctx") {
        "repository_ctx.extract"(
          "archive" to !"repository_ctx.attr.path",
        )
      }

      "archive_repository=repository_rule"(
        "implementation" to !"_archive_repository_impl",
        "attrs" to !"{'path': attr.string()}"
      )
    }
    return "@//:archive_repository.bzl" to "archive_repository"
  }
}
