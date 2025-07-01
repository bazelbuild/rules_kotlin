package io.bazel.kotlin

import org.junit.Test
import kotlin.test.assertContains

class KotlinJsAssertionTest: KotlinAssertionTestCase("src/test/data/js") {

  @Test
  fun testBasicTargetHasKlibManifest() {
    zipTestCase(
      name = "singleunit/singleunit.klib",
      description = "Single kt_js_library target produces valid klib"
    ) {
      assertContainsEntries(
        "default/manifest",
      )

      val entry = getEntry("default/manifest")
      getInputStream(entry).bufferedReader().use {
        val manifestContent = it.readText()
        assertContains(manifestContent, "builtins_platform=JS")
        assertContains(manifestContent, "compiler_version=2.1.21")
        assertContains(manifestContent, "unique_name=src_test_data_js_singleunit")
      }
    }
  }


  @Test
  fun testTargetWithDepsHasKlibManifest() {
    zipTestCase(
      name = "deps/main.klib",
      description = "Single kt_js_library target with deps produces valid klib"
    ) {
      assertContainsEntries(
        "default/manifest",
      )

      val entry = getEntry("default/manifest")
      getInputStream(entry).bufferedReader().use {
        val manifestContent = it.readText()
        assertContains(manifestContent, "builtins_platform=JS")
        assertContains(manifestContent, "compiler_version=2.1.21")
        assertContains(manifestContent, "unique_name=src_test_data_js_deps-main")
      }
    }
  }
}
