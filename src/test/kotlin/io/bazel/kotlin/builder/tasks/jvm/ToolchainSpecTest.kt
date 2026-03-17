package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.toolchain.InternalCompilerPlugin
import io.bazel.kotlin.builder.toolchain.ToolchainSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path

@RunWith(JUnit4::class)
class ToolchainSpecTest {
  @Test
  fun `equality is based on btapiClasspath and plugins`() {
    val spec1 = ToolchainSpec(
      listOf(Path.of("/a.jar"), Path.of("/b.jar")),
      mapOf("jdeps" to InternalCompilerPlugin("/jdeps.jar", "jdeps.id")),
    )
    val spec2 = ToolchainSpec(
      listOf(Path.of("/a.jar"), Path.of("/b.jar")),
      mapOf("jdeps" to InternalCompilerPlugin("/jdeps.jar", "jdeps.id")),
    )
    val spec3 = ToolchainSpec(
      listOf(Path.of("/a.jar"), Path.of("/c.jar")),
      mapOf("jdeps" to InternalCompilerPlugin("/jdeps.jar", "jdeps.id")),
    )

    assertThat(spec1).isEqualTo(spec2)
    assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode())
    assertThat(spec1).isNotEqualTo(spec3)
  }

  @Test
  fun `classpath order matters for equality`() {
    val plugins = mapOf("jdeps" to InternalCompilerPlugin("/jdeps.jar", "jdeps.id"))
    val spec1 = ToolchainSpec(listOf(Path.of("/a.jar"), Path.of("/b.jar")), plugins)
    val spec2 = ToolchainSpec(listOf(Path.of("/b.jar"), Path.of("/a.jar")), plugins)

    assertThat(spec1).isNotEqualTo(spec2)
  }

  @Test
  fun `requirePlugin returns plugin when present`() {
    val plugin = InternalCompilerPlugin("/jdeps.jar", "jdeps.id")
    val spec = ToolchainSpec(emptyList(), mapOf(ToolchainSpec.JDEPS to plugin))

    assertThat(spec.requirePlugin(ToolchainSpec.JDEPS)).isEqualTo(plugin)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `requirePlugin throws when plugin is missing`() {
    val spec = ToolchainSpec(emptyList(), emptyMap())
    spec.requirePlugin(ToolchainSpec.JDEPS)
  }
}
