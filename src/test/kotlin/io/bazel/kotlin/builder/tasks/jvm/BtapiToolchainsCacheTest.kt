package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.Deps
import io.bazel.kotlin.builder.toolchain.BtapiRuntimeSpec
import io.bazel.kotlin.builder.toolchain.BtapiToolchainsCache
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalBuildToolsApi::class)
@RunWith(JUnit4::class)
class BtapiToolchainsCacheTest {
  companion object {
    private fun btapiRuntimeSpec(): BtapiRuntimeSpec =
      BtapiRuntimeSpec(
        listOf(
          Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_build_tools_impl").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_compiler_embeddable").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_daemon_client").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-reflect").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlinx-coroutines-core-jvm").singleCompileJar()),
          Path.of(Deps.Dep.fromLabel("//kotlin/compiler:annotations").singleCompileJar()),
        ),
      )
  }

  private val cache = BtapiToolchainsCache()

  @Test
  fun `get returns same instance for same runtime spec`() {
    val spec = btapiRuntimeSpec()
    val toolchains1 = cache.get(spec)
    val toolchains2 = cache.get(spec)
    assertThat(toolchains1).isSameInstanceAs(toolchains2)
  }

  @Test
  fun `get returns same instance for equal specs`() {
    val spec1 = btapiRuntimeSpec()
    val spec2 = btapiRuntimeSpec()
    assertThat(spec1).isEqualTo(spec2)
    val toolchains1 = cache.get(spec1)
    val toolchains2 = cache.get(spec2)
    assertThat(toolchains1).isSameInstanceAs(toolchains2)
  }

  @Test
  fun `get throws when classpath file does not exist`() {
    val spec = BtapiRuntimeSpec(listOf(Path.of("/nonexistent/path/does-not-exist.jar")))
    val exception = assertThrows(IllegalArgumentException::class.java) {
      cache.get(spec)
    }
    assertThat(exception).hasMessageThat().contains("does not exist or is not a file")
    assertThat(exception).hasMessageThat().contains("does-not-exist.jar")
  }

  @Test
  fun `get throws when classpath entry is a directory not a file`() {
    val tempDir = Files.createTempDirectory("btapi-cache-test")
    try {
      val spec = BtapiRuntimeSpec(listOf(tempDir))
      val exception = assertThrows(IllegalArgumentException::class.java) {
        cache.get(spec)
      }
      assertThat(exception).hasMessageThat().contains("does not exist or is not a file")
    } finally {
      Files.deleteIfExists(tempDir)
    }
  }
}
