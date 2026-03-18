/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
import io.bazel.kotlin.model.JvmCompilationTask
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PluginsPayloadParserTest {
  @Test
  fun `parses plugins payload json`() {
    val payload =
      PluginsPayloadParser.parse(
        """
        {
          "stubs_plugins": [
            {
              "id": "plugin.stubs",
              "classpath": ["stub.jar"],
              "options": [
                {"key": "stubKey", "value": "stubValue"}
              ]
            }
          ],
          "compiler_plugins": [
            {
              "id": "plugin.compile",
              "classpath": ["a.jar", "b.jar"],
              "options": [
                {"key": "k1", "value": "v1"},
                {"key": "k2", "value": "v2"}
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertThat(payload.stubsPluginsList).hasSize(1)
    assertThat(payload.compilerPluginsList).hasSize(1)

    val stubsPlugin = payload.stubsPluginsList.single()
    assertThat(stubsPlugin.id).isEqualTo("plugin.stubs")
    assertThat(stubsPlugin.classpathList).containsExactly("stub.jar")
    assertThat(stubsPlugin.optionsList.map { "${it.key}=${it.value}" })
      .containsExactly("stubKey=stubValue")

    val compilerPlugin = payload.compilerPluginsList.single()
    assertThat(compilerPlugin.id).isEqualTo("plugin.compile")
    assertThat(compilerPlugin.classpathList).containsExactly("a.jar", "b.jar").inOrder()
    assertThat(compilerPlugin.optionsList.map { "${it.key}=${it.value}" })
      .containsExactly("k1=v1", "k2=v2")
      .inOrder()
  }

  @Test
  fun `ignores unknown json fields`() {
    val payload =
      PluginsPayloadParser.parse(
        """
        {
          "unknown_top_level": "ignored",
          "compiler_plugins": [
            {
              "id": "plugin.unknown",
              "classpath": [],
              "options": [],
              "unknown_nested": "ignored"
            }
          ]
        }
        """.trimIndent(),
      )

    assertThat(payload.compilerPluginsList).hasSize(1)
    assertThat(payload.compilerPluginsList.single().id).isEqualTo("plugin.unknown")
  }

  @Test
  fun `rejects invalid json`() {
    try {
      PluginsPayloadParser.parse("{invalid")
      fail("Expected parse to fail")
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains("invalid plugins payload JSON")
    }
  }
}
