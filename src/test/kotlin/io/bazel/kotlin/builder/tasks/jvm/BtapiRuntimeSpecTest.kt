package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.toolchain.BtapiRuntimeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path

@RunWith(JUnit4::class)
class BtapiRuntimeSpecTest {
  @Test
  fun `fromClasspathEntries converts strings to paths`() {
    val spec = BtapiRuntimeSpec.fromClasspathEntries(listOf("/a.jar", "/b.jar"))
    assertThat(spec.classpath).containsExactly(Path.of("/a.jar"), Path.of("/b.jar")).inOrder()
  }

  @Test
  fun `equality is based on classpath content`() {
    val spec1 = BtapiRuntimeSpec.fromClasspathEntries(listOf("/a.jar", "/b.jar"))
    val spec2 = BtapiRuntimeSpec.fromClasspathEntries(listOf("/a.jar", "/b.jar"))
    val spec3 = BtapiRuntimeSpec.fromClasspathEntries(listOf("/a.jar", "/c.jar"))

    assertThat(spec1).isEqualTo(spec2)
    assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode())
    assertThat(spec1).isNotEqualTo(spec3)
  }

  @Test
  fun `classpath order matters for equality`() {
    val spec1 = BtapiRuntimeSpec.fromClasspathEntries(listOf("/a.jar", "/b.jar"))
    val spec2 = BtapiRuntimeSpec.fromClasspathEntries(listOf("/b.jar", "/a.jar"))

    assertThat(spec1).isNotEqualTo(spec2)
  }

  @Test
  fun `empty classpath produces empty spec`() {
    val spec = BtapiRuntimeSpec.fromClasspathEntries(emptyList())
    assertThat(spec.classpath).isEmpty()
  }
}
