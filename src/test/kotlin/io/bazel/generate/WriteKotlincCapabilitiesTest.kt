package io.bazel.generate

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.generate.WriteKotlincCapabilities
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.junit.Test
import java.nio.file.Files

class WriteKotlincCapabilitiesTest {
  // Use the latest supported version for testing
  private val testVersion = KotlinReleaseVersion.v2_3_0

  @Test
  fun smokeTest() {
    var tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())
    val got = tmp.resolve(WriteKotlincCapabilities.capabilitiesName(testVersion))
    assertThat(Files.exists(got)).isTrue()
    // assert stable flag from kotlin-compiler-arguments-description
    assertThat(Files.readString(got)).contains("-progressive")

    // also check generated_opts file was created
    val generatedOpts = tmp.resolve(WriteKotlincCapabilities.generatedOptsName(testVersion))
    assertThat(Files.exists(generatedOpts)).isTrue()
    assertThat(Files.readString(generatedOpts)).contains("GENERATED_KOPTS")
  }

  @Test
  fun `boolean options generate bool structure`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName(testVersion)))

    // Boolean options should use attr.bool
    assertThat(generatedOpts).contains("type = attr.bool")

    // Boolean options should map True to the flag
    assertThat(generatedOpts).contains("value_to_flag = {True:")
  }

  @Test
  fun `string options use map_value_to_flag`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName(testVersion)))

    // String options should use _map_string_flag helper
    assertThat(generatedOpts).contains("map_value_to_flag = _map_string_flag")

    // The helper function should be defined
    assertThat(generatedOpts).contains("def _map_string_flag(flag):")
  }

  @Test
  fun `string list options use map_string_list_flag`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName(testVersion)))

    // String list options should use _map_string_list_flag helper
    assertThat(generatedOpts).contains("map_value_to_flag = _map_string_list_flag")

    // The helper function should be defined
    assertThat(generatedOpts).contains("def _map_string_list_flag(flag):")

    // String list options should have type = attr.string_list
    assertThat(generatedOpts).contains("type = attr.string_list")
  }

  @Test
  fun `generates files for all supported versions`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    for (version in WriteKotlincCapabilities.SUPPORTED_VERSIONS) {
      val capabilitiesFile = tmp.resolve(WriteKotlincCapabilities.capabilitiesName(version))
      assertThat(Files.exists(capabilitiesFile))
        .named("capabilities file for ${version.major}.${version.minor}")
        .isTrue()

      val generatedOptsFile = tmp.resolve(WriteKotlincCapabilities.generatedOptsName(version))
      assertThat(Files.exists(generatedOptsFile))
        .named("generated_opts file for ${version.major}.${version.minor}")
        .isTrue()
    }
  }

  @Test
  fun `version filtering works correctly`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    // XXlenient-mode was introduced in v2.2.0 - should be in 2.2 and 2.3 but not in 2.0 and 2.1
    val opts20 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_0_0)))
    val opts21 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_1_0)))
    val opts22 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_2_0)))
    val opts23 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_3_0)))

    assertThat(opts20).doesNotContain("-XXlenient-mode")
    assertThat(opts21).doesNotContain("-XXlenient-mode")
    assertThat(opts22).contains("-XXlenient-mode")
    assertThat(opts23).contains("-XXlenient-mode")
  }

  @Test
  fun `experimental flags marked deprecated when stable counterpart exists`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    // -jvm-default was introduced in 2.2, -Xjvm-default exists since 1.2
    // In 2.1: only -Xjvm-default exists, no deprecation prefix from us
    // In 2.2+: both exist, -Xjvm-default should have our DEPRECATED prefix

    val opts21 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_1_0)))
    val opts22 = Files.readString(tmp.resolve(WriteKotlincCapabilities.capabilitiesName(KotlinReleaseVersion.v2_2_0)))

    // In 2.1, -Xjvm-default should NOT have our DEPRECATED prefix (stable version doesn't exist yet)
    assertThat(opts21).contains("-Xjvm-default")
    assertThat(opts21).doesNotContain("DEPRECATED: Use -jvm-default instead")

    // In 2.2, -Xjvm-default SHOULD have our DEPRECATED prefix (stable -jvm-default now exists)
    assertThat(opts22).contains("-Xjvm-default")
    assertThat(opts22).contains("DEPRECATED: Use -jvm-default instead")

    // Also verify -jvm-default exists in 2.2
    assertThat(opts22).contains("\"-jvm-default\"")
  }
}
