package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalCompilerPluginsTest {
  @Test
  fun `fromPaths creates plugins with correct IDs`() {
    val plugins = InternalCompilerPlugins.fromPaths(
      "/abi-gen.jar",
      "/skip-code-gen.jar",
      "/kapt.jar",
      "/jdeps.jar",
    )

    assertThat(plugins.jvmAbiGen.id).isEqualTo(InternalCompilerPlugins.JVM_ABI_GEN_ID)
    assertThat(plugins.skipCodeGen.id).isEqualTo(InternalCompilerPlugins.SKIP_CODE_GEN_ID)
    assertThat(plugins.kapt.id).isEqualTo(InternalCompilerPlugins.KAPT_ID)
    assertThat(plugins.jdeps.id).isEqualTo(InternalCompilerPlugins.JDEPS_ID)
  }

  @Test
  fun `plugin IDs match expected values`() {
    assertThat(InternalCompilerPlugins.JVM_ABI_GEN_ID).isEqualTo("org.jetbrains.kotlin.jvm.abi")
    assertThat(InternalCompilerPlugins.SKIP_CODE_GEN_ID).isEqualTo("io.bazel.kotlin.plugin.SkipCodeGen")
    assertThat(InternalCompilerPlugins.KAPT_ID).isEqualTo("org.jetbrains.kotlin.kapt3")
    assertThat(InternalCompilerPlugins.JDEPS_ID).isEqualTo("io.bazel.kotlin.plugin.jdeps.JDepsGen")
  }

  @Test
  fun `fromPaths preserves jar paths`() {
    val plugins = InternalCompilerPlugins.fromPaths(
      "/path/to/abi-gen.jar",
      "/path/to/skip-code-gen.jar",
      "/path/to/kapt.jar",
      "/path/to/jdeps.jar",
    )

    assertThat(plugins.jvmAbiGen.jarPath).isEqualTo("/path/to/abi-gen.jar")
    assertThat(plugins.skipCodeGen.jarPath).isEqualTo("/path/to/skip-code-gen.jar")
    assertThat(plugins.kapt.jarPath).isEqualTo("/path/to/kapt.jar")
    assertThat(plugins.jdeps.jarPath).isEqualTo("/path/to/jdeps.jar")
  }

  @Test
  fun `InternalCompilerPlugin data class equality works`() {
    val plugin1 = InternalCompilerPlugin("/a.jar", "my.plugin")
    val plugin2 = InternalCompilerPlugin("/a.jar", "my.plugin")
    val plugin3 = InternalCompilerPlugin("/b.jar", "my.plugin")

    assertThat(plugin1).isEqualTo(plugin2)
    assertThat(plugin1).isNotEqualTo(plugin3)
  }
}
