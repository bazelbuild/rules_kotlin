package io.bazel.kotlin

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinKlibAssertionTest : KotlinAssertionTestCase("src/test/data/native") {
  @Test
  fun testBasicKlibIsProduced() {
    klibTestCase("basic.klib", "kt_lib_library produces klib with stdlib usage") {
      assertContainsEntries(
        "default/linkdata/package_basic/0_basic.knm",
        "default/manifest",
      )
    }
  }

  @Test
  fun testDepsKlibIsProduced() {
    klibTestCase("deps_main.klib", "kt_lib_library produces klib with stdlib usage") {
      assertContainsEntries(
        "default/linkdata/package_main/0_main.knm",
        "default/manifest",
      )
    }
  }
}
