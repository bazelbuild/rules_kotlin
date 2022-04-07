package io.bazel.kotlin.integration

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.integration.RulesKotlinWorkspace.Companion.build
import org.junit.Test
import java.nio.file.Files

class MixedSourceCompileTest {

  @Test
  fun `kotlin and java sources with dependency`() {
    val workspace = RulesKotlinWorkspace.write(rulesRepo = "rules_kotlin_release") {

      workspace {
        val (kt_register_toolchains) = load(
          "@rules_kotlin_release//kotlin:core.bzl",
          "kt_register_toolchains"
        )
        kt_register_toolchains()
      }

      "library" {
        build {
          val (ktJvmLibrary) = load("@rules_kotlin_release//kotlin:jvm.bzl", "kt_jvm_library")

          ktJvmLibrary(
            "name" to "library",
            "srcs" to !"['Library.kt']",
            "visibility" to !"['//:__subpackages__']"
          )
        }
        kotlin("Library.kt") {
          `package`("library")
          `class`("Library") {}
        }
      }

      "mixed" {
        build {
          val (ktJvmLibrary) = load("@rules_kotlin_release//kotlin:jvm.bzl", "kt_jvm_library")

          ktJvmLibrary(
            "name" to "mixed",
            "srcs".list("Main.java", "Dep.kt"),
            "visibility" to !"['//:__subpackages__']",
            "deps".list("//library")
          )
        }

        java("Main.java") {
          `package`("mixed")

          "public class Main" {
            "public void run()" {
              +"new Dep();"
            }
          }
        }

        kotlin("Dep.kt") {
          `package`("mixed")
          `class`("Dep") {}
        }
      }
    }

    val result = workspace.build("//mixed")
    assertWithMessage("failed with $result").that(result.exit).isEqualTo(0)
  }
}
