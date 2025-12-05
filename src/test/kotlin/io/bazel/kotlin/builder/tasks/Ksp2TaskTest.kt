/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.bazel.kotlin.builder.tasks

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.KotlinBuilderFlags
import io.bazel.kotlin.builder.utils.ArgMap
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for KSP2 task argument handling in KotlinBuilder.
 */
@RunWith(JUnit4::class)
class Ksp2TaskTest {
  @Test
  fun testKsp2ModeFlag() {
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_MODE.flag to listOf("true"),
        ),
      )
    assertThat(args.optionalSingle(KotlinBuilderFlags.KSP2_MODE)).isEqualTo("true")
  }

  @Test
  fun testKsp2ModuleName() {
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_MODULE_NAME.flag to listOf("test_module"),
        ),
      )
    assertThat(args.mandatorySingle(KotlinBuilderFlags.KSP2_MODULE_NAME)).isEqualTo("test_module")
  }

  @Test
  fun testKsp2Sources() {
    val sources = listOf("src/Foo.kt", "src/Bar.java")
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_SOURCES.flag to sources,
        ),
      )
    assertThat(args.optional(KotlinBuilderFlags.KSP2_SOURCES)).containsExactlyElementsIn(sources)
  }

  @Test
  fun testKsp2SourceJars() {
    val srcjars = listOf("lib1-sources.jar", "lib2-sources.jar")
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_SOURCE_JARS.flag to srcjars,
        ),
      )
    assertThat(args.optional(KotlinBuilderFlags.KSP2_SOURCE_JARS))
      .containsExactlyElementsIn(srcjars)
  }

  @Test
  fun testKsp2Libraries() {
    val libs = listOf("dep1.jar", "dep2.jar", "dep3.jar")
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_LIBRARIES.flag to libs,
        ),
      )
    assertThat(args.optional(KotlinBuilderFlags.KSP2_LIBRARIES)).containsExactlyElementsIn(libs)
  }

  @Test
  fun testKsp2ProcessorClasspath() {
    val processorJars = listOf("processor1.jar", "ksp-api.jar")
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_PROCESSOR_CLASSPATH.flag to processorJars,
        ),
      )
    assertThat(args.optional(KotlinBuilderFlags.KSP2_PROCESSOR_CLASSPATH))
      .containsExactlyElementsIn(processorJars)
  }

  @Test
  fun testKsp2GeneratedOutputPaths() {
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_GENERATED_SOURCES_OUTPUT.flag to listOf("gen-sources.jar"),
          KotlinBuilderFlags.KSP2_GENERATED_CLASSES_OUTPUT.flag to listOf("gen-classes.jar"),
        ),
      )
    assertThat(args.mandatorySingle(KotlinBuilderFlags.KSP2_GENERATED_SOURCES_OUTPUT))
      .isEqualTo("gen-sources.jar")
    assertThat(args.mandatorySingle(KotlinBuilderFlags.KSP2_GENERATED_CLASSES_OUTPUT))
      .isEqualTo("gen-classes.jar")
  }

  @Test
  fun testKsp2CompilerSettings() {
    val args =
      ArgMap(
        mapOf(
          KotlinBuilderFlags.KSP2_LANGUAGE_VERSION.flag to listOf("1.9"),
          KotlinBuilderFlags.KSP2_API_VERSION.flag to listOf("1.9"),
          KotlinBuilderFlags.KSP2_JVM_TARGET.flag to listOf("17"),
          KotlinBuilderFlags.KSP2_JDK_HOME.flag to listOf("/path/to/jdk"),
        ),
      )
    assertThat(args.optionalSingle(KotlinBuilderFlags.KSP2_LANGUAGE_VERSION)).isEqualTo("1.9")
    assertThat(args.optionalSingle(KotlinBuilderFlags.KSP2_API_VERSION)).isEqualTo("1.9")
    assertThat(args.optionalSingle(KotlinBuilderFlags.KSP2_JVM_TARGET)).isEqualTo("17")
    assertThat(args.optionalSingle(KotlinBuilderFlags.KSP2_JDK_HOME)).isEqualTo("/path/to/jdk")
  }

  @Test
  fun testKsp2FlagsHaveCorrectNames() {
    // Verify flag names match expected format
    assertThat(KotlinBuilderFlags.KSP2_MODE.flag).isEqualTo("--ksp2_mode")
    assertThat(KotlinBuilderFlags.KSP2_MODULE_NAME.flag).isEqualTo("--ksp2_module_name")
    assertThat(KotlinBuilderFlags.KSP2_SOURCES.flag).isEqualTo("--ksp2_sources")
    assertThat(KotlinBuilderFlags.KSP2_SOURCE_JARS.flag).isEqualTo("--ksp2_source_jars")
    assertThat(KotlinBuilderFlags.KSP2_LIBRARIES.flag).isEqualTo("--ksp2_libraries")
    assertThat(KotlinBuilderFlags.KSP2_PROCESSOR_CLASSPATH.flag)
      .isEqualTo("--ksp2_processor_classpath")
    assertThat(KotlinBuilderFlags.KSP2_GENERATED_SOURCES_OUTPUT.flag)
      .isEqualTo("--ksp2_generated_sources_output")
    assertThat(KotlinBuilderFlags.KSP2_GENERATED_CLASSES_OUTPUT.flag)
      .isEqualTo("--ksp2_generated_classes_output")
    assertThat(KotlinBuilderFlags.KSP2_LANGUAGE_VERSION.flag).isEqualTo("--ksp2_language_version")
    assertThat(KotlinBuilderFlags.KSP2_API_VERSION.flag).isEqualTo("--ksp2_api_version")
    assertThat(KotlinBuilderFlags.KSP2_JVM_TARGET.flag).isEqualTo("--ksp2_jvm_target")
    assertThat(KotlinBuilderFlags.KSP2_JDK_HOME.flag).isEqualTo("--ksp2_jdk_home")
  }

  @Test
  fun testEmptyOptionalSources() {
    val args = ArgMap(mapOf())
    assertThat(args.optional(KotlinBuilderFlags.KSP2_SOURCES)).isNull()
    assertThat(args.optional(KotlinBuilderFlags.KSP2_SOURCE_JARS)).isNull()
  }
}
