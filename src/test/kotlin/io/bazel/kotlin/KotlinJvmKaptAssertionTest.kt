/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin

import org.junit.jupiter.api.Test;

class KotlinJvmKaptAssertionTest : KotlinAssertionTestCase("src/test/data/jvm/kapt") {
  @Test
  fun testKotlinOnlyAnnotationProcessing() {
    jarTestCase("ap_kotlin.jar", description = "annotation processing should work") {
      assertContainsEntries("tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class")
    }
    jarTestCase(
      "ap_kotlin_mixed_no_plugin.jar",
      description = "annotation processing should not kick in for deps."
    ) {
      assertDoesNotContainEntries(
        "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
        "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
      )
    }
    jarTestCase(
      "ap_kotlin_resources.jar",
      description = "annotation processed artifacts should contain resources"
    ) {
      assertContainsEntries("META-INF/services/tests.smoke.kapt.kotlin.TestKtService")
    }
  }

  @Test
  fun testMixedModeAnnotationProcessing() {
    jarTestCase(
      "ap_kotlin_mixed.jar",
      description = "annotation processing should work for mixed mode targets"
    ) {
      assertContainsEntries(
        "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
        "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
      )
    }
    jarTestCase(
      "ap_kotlin_resources_mixed.jar",
      description = "annotation processors generating resources should work for mixed mode"
    ) {
      assertContainsEntries(
        "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
        "META-INF/services/tests.smoke.kapt.java.TestJavaService"
      )
    }
    jarTestCase(
      "ap_kotlin_mixed_inherit_plugin_via_exported_deps.jar",
      description = "annotation processors should be inherited transitively"
    ) {
      assertContainsEntries(
        "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
        "META-INF/services/tests.smoke.kapt.java.TestJavaService",
        "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
        "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
      )
    }
  }

  @Test
  fun testMultiPlugins() {
    jarTestCase(
      "ap_kotlin_mixed_multiple_plugins.jar",
      description = "annotation processing should work for multiple plugins"
    ) {
      assertContainsEntries(
        "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
        "META-INF/services/tests.smoke.kapt.java.TestJavaService",
        "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
        "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
      )
    }
    jarTestCase(
      "ap_kotlin_mixed_multiple_plugins_one_without_processor_class.jar",
      description = "annotation processing should not work unless a processor class is provided"
    ) {
      assertContainsEntries(
        "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
        "META-INF/services/tests.smoke.kapt.java.TestJavaService"
      )
      assertDoesNotContainEntries(
        "tests/smoke/kapt/java/AutoValue_TestAPNoGenReferences.class",
        "tests/smoke/kapt/kotlin/AutoValue_TestKtValueNoReferences.class"
      )
    }
  }

  @Test
  fun testSrcJarGeneration() {
    jarTestCase(
      "ap_kotlin_mixed_multiple_plugins-sources.jar",
      description = "The rules should generate a source jar"
    ) {
      assertContainsEntries(
        "test/data/jvm/kapt/java/TestAutoValue.java",
        "test/data/jvm/kapt/java/TestJavaService.java",
        "test/data/jvm/kapt/kotlin/TestKtService.kt",
        "test/data/jvm/kapt/kotlin/TestKtValue.kt"
      )
    }
  }
}
