package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.toolchain.InternalCompilerPlugin
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalCompilerPluginTest {
  @Test
  fun `data class equality works`() {
    val plugin1 = InternalCompilerPlugin("/a.jar", "my.plugin")
    val plugin2 = InternalCompilerPlugin("/a.jar", "my.plugin")
    val plugin3 = InternalCompilerPlugin("/b.jar", "my.plugin")

    assertThat(plugin1).isEqualTo(plugin2)
    assertThat(plugin1.hashCode()).isEqualTo(plugin2.hashCode())
    assertThat(plugin1).isNotEqualTo(plugin3)
  }

  @Test
  fun `preserves jar path and id`() {
    val plugin = InternalCompilerPlugin("/path/to/plugin.jar", "org.example.plugin")
    assertThat(plugin.jarPath).isEqualTo("/path/to/plugin.jar")
    assertThat(plugin.id).isEqualTo("org.example.plugin")
  }
}
