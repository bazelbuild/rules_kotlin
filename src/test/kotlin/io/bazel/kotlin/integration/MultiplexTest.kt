package io.bazel.kotlin.integration

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.newOutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8

class MultiplexTest {

  private val rulesRepo = "rules_kotlin_release"

  private fun writeReleaseArchive(): Path {
    return MultiplexTest::class.java.classLoader.getResourceAsStream("_release.tgz")
      ?.let { stream ->
        createTempDirectory("rules_kotlin_release").resolve("rules_kotlin_release.tgz").also {
          newOutputStream(it, CREATE_NEW).buffered().write(stream.readAllBytes())
        }
      }
      ?: error("Cannot find release repo")
  }


  private fun WriteWorkspace.Workspace.defineArchiveRepository(): Pair<String, String> {
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

  private fun WriteWorkspace.BuildBazel.loadKtJvmLibrary(): String {
    load("@$rulesRepo//kotlin:jvm.bzl", "kt_jvm_library")
    return "kt_jvm_library"
  }

  private data class BuildResult(
    val code: Int,
    val out: String,
    val err: String,
  )

  private fun Path.build(vararg targets: String): BuildResult {
    val out = Files.createTempFile("out", "txt").toFile().apply {
      deleteOnExit()
    }
    val err = Files.createTempFile("out", "txt").toFile().apply {
      deleteOnExit()
    }

    return ProcessBuilder()
      .command(listOf("bazel", "build", "--experimental_worker_max_multiplex_instances", "1") + targets)
      .redirectOutput(out)
      .redirectError(err)
      .directory(this.toFile())
      .start().runCatching {
        if (!waitFor(5, TimeUnit.MINUTES)) {
          error("build took too long:\nout: ${out.readText(UTF_8)}\nerr: ${err.readText(UTF_8)}")
        }
        BuildResult(
          code = exitValue(),
          out = out.readText(UTF_8),
          err = err.readText(UTF_8)
        )
      }
      .recover { exception ->
        BuildResult(
          code = 1,
          out = "",
          err = exception.toString()
        )
      }
      .getOrThrow()
  }

  fun <T : WriteWorkspace.SubPackage<T>> T.one(contents: T.() -> Unit): WriteWorkspace.Resolve {
    return "library/one"{
      apply(contents)
    }
  }

  fun <T : WriteWorkspace.SubPackage<T>> T.two(contents: T.() -> Unit): WriteWorkspace.Resolve {
    return "library/two"{
      apply(contents)
    }
  }

  @Test
  fun multipleBuilds() {
    val workspace = WriteWorkspace.using<MultiplexTest> {
      build {
        val (define_kt_toolchain) = load("@$rulesRepo//kotlin:core.bzl", "define_kt_toolchain")
        define_kt_toolchain(
          "experimental_multiplex_workers" to True
        )
      }

      val (location, archive_repository) = defineArchiveRepository()
      workspace {
        load(location, archive_repository)
        archive_repository(
          "name" to rulesRepo,
          "path" to writeReleaseArchive().toString()
        )
        load("@$rulesRepo//kotlin:repositories.bzl", "kotlin_repositories")
        "kotlin_repositories"()

        load("@$rulesRepo//kotlin:core.bzl", "kt_register_toolchains")
        "kt_register_toolchains"()
      }

      val lib = one {
        build {
          val ktJvmLibrary = loadKtJvmLibrary()

          ktJvmLibrary(
            "name" to "one",
            "srcs" to !"glob(['*.kt'])",
            "visibility" to !"['//:__subpackages__']"
          )
        }

        kotlin("One.kt") {
          +"package library.one"
          "interface One" {
            "fun one"(!"value:String")
          }
        }
      }

      two {
        build {
          val ktJvmLibrary = loadKtJvmLibrary()

          ktJvmLibrary(
            "name" to "two",
            "srcs" to !"glob(['*.kt'])",
            "deps" to !"[${lib.target("one")}]",
            "visibility" to !"['//:__subpackages__']"
          )
        }
        kotlin("Two.kt") {
          +"package library.two"
          +"import library.one.One"
          "class Two : One" {
            "override fun one"(!"value:String") {
              "println"(!"value")
            }
          }
        }
      }
    }

    (0..50).zipWithNext().forEach { (last, next) ->
      workspace.build("//library/two").run {
        assertWithMessage("Out:\n$out\nErr:\n$err").that(code).isEqualTo(0)
        println("$next:\n$err\n\n")
      }
      WriteWorkspace.open(workspace) {
        one {
          kotlin("One$next.kt") {
            +"package library.one"
            "interface One$next" {
              "fun one"(!"context:One$next.()->Unit")
            }
          }
        }
        two {
          kotlin("Two.kt") {
            +"package library.two"
            +"import library.one.One$next"
            "class Two : One$next" {
              "override fun one"(!"context:One$next.()->Unit") {
                "apply"(!"context")
              }
            }
          }
        }
      }
    }
  }
}
