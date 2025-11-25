package io.bazel.generate

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.generate.WriteKotlincCapabilities
import org.junit.Test
import java.nio.file.Files

class WriteKotlincCapabilitiesTest {
  @Test
  fun smokeTest() {
    var tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())
    val got = tmp.resolve(WriteKotlincCapabilities.capabilitiesName)
    assertThat(Files.exists(got)).isTrue()
    // assert stable flag from kotlin-compiler-arguments-description
    assertThat(Files.readString(got)).contains("-jvm-target")

    // also check generated_opts file was created
    val generatedOpts = tmp.resolve(WriteKotlincCapabilities.generatedOptsName)
    assertThat(Files.exists(generatedOpts)).isTrue()
    assertThat(Files.readString(generatedOpts)).contains("GENERATED_KOPTS")
  }

  @Test
  fun `boolean options generate tristate structure`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName))

    // Boolean options should use attr.string (not attr.bool) for tristate
    assertThat(generatedOpts).contains("type = attr.string")

    // Boolean options should have values = ["true", "false"] for tristate
    assertThat(generatedOpts).contains("""values = ["true", "false"]""")

    // Boolean options should only map "true" to the flag (empty/unset = don't pass)
    // Find a known boolean flag like -Xallow-no-source-files
    assertThat(generatedOpts).contains(""""true": ["-X""")

    // Verify the tristate doc string is present
    assertThat(generatedOpts).contains("unset = use kotlinc default")
  }

  @Test
  fun `string options use map_value_to_flag`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName))

    // String options should use _map_string_flag helper
    assertThat(generatedOpts).contains("map_value_to_flag = _map_string_flag")

    // The helper function should be defined
    assertThat(generatedOpts).contains("def _map_string_flag(flag):")
  }

  @Test
  fun `string list options use map_string_list_flag`() {
    val tmp = Files.createTempDirectory("WriteKotlincCapabilitiesTest")
    WriteKotlincCapabilities.main("--out", tmp.toString())

    val generatedOpts = Files.readString(tmp.resolve(WriteKotlincCapabilities.generatedOptsName))

    // String list options should use _map_string_list_flag helper
    assertThat(generatedOpts).contains("map_value_to_flag = _map_string_list_flag")

    // The helper function should be defined
    assertThat(generatedOpts).contains("def _map_string_list_flag(flag):")

    // String list options should have type = attr.string_list
    assertThat(generatedOpts).contains("type = attr.string_list")
  }
}
