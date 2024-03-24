package io.bazel.kotlin.integration

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.integration.WriteWorkspace.Indenting
import io.bazel.kotlin.integration.WriteWorkspace.Text
import org.junit.Test
import java.nio.file.Files.exists

class WriteWorkspaceTest {
  @Test
  fun indent() {
    val out = StringBuilder()
    object : Text by Indenting(out) {}.apply {
      +"foozle"
      indent {
        +"fizzle"
        indent {
          +"fop"
        }
      }
    }

    assertThat(out.toString()).isEqualTo(
      """|foozle
         |  fizzle
         |    fop
         |""".trimMargin())
  }

  @Test
  fun bzlLoad() {
    val out = StringBuilder()
    object : WriteWorkspace.BzlWorkspace, Text by Indenting(out) {}.apply {
      load("@a_repo//in/a:place.bzl", "here", "no", "there")
    }
    assertThat(out.toString()).isEqualTo(
      """|load("@a_repo//in/a:place.bzl", "here", "no", "there")
         |""".trimMargin())
  }

  @Test
  fun starklarkBlock() {
    val out = StringBuilder()
    object : WriteWorkspace.Bzl, Text by Indenting(out) {}.apply {
      "def scooby"(!"dooby") {
        "print"("%s doo" % !"dooby")
      }
    }
    assertThat(out.toString()).isEqualTo(
      """|def scooby(dooby):
         |  print("%s doo" % dooby)
         |""".trimMargin())
  }

  @Test
  fun alwaysWriteBuild() {
    val workspace = WriteWorkspace.using<WriteWorkspaceTest> {}
    assertThat(exists(workspace.resolve("WORKSPACE"))).isTrue()
    assertThat(exists(workspace.resolve("BUILD.bazel"))).isTrue()
  }
}
