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
package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrder
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@RunWith(JUnit4::class)
class BtapiPluginArgumentsTest {
  @Test
  fun `converts plugin classpath and options to raw argument strings`() {
    val firstJar = Path.of("plugin-one.jar").toAbsolutePath()
    val secondJar = Path.of("plugin-two.jar").toAbsolutePath()

    val args =
      BtapiPluginArguments.toArgumentStrings(
        listOf(
          CompilerPlugin(
            pluginId = "plugin.one",
            classpath = listOf(firstJar),
            rawArguments =
              listOf(
                CompilerPluginOption("opt1", "v1"),
                CompilerPluginOption("opt2", "v2"),
              ),
            orderingRequirements = emptySet(),
          ),
          CompilerPlugin(
            pluginId = "plugin.two",
            classpath = listOf(secondJar),
            rawArguments = listOf(CompilerPluginOption("flag", "true")),
            orderingRequirements = emptySet(),
          ),
        ),
      )

    assertThat(args).containsExactly(
      "-Xplugin=${firstJar.absolutePathString()},${secondJar.absolutePathString()}",
      "-P",
      "plugin:plugin.one:opt1=v1,plugin:plugin.one:opt2=v2,plugin:plugin.two:flag=true",
    ).inOrder()
  }

  @Test
  fun `converts plugin order constraints`() {
    val firstJar = Path.of("plugin-one.jar").toAbsolutePath()
    val secondJar = Path.of("plugin-two.jar").toAbsolutePath()

    val args =
      BtapiPluginArguments.toArgumentStrings(
        listOf(
          CompilerPlugin(
            pluginId = "plugin.one",
            classpath = listOf(firstJar),
            rawArguments = emptyList(),
            orderingRequirements =
              setOf(
                CompilerPluginPartialOrder(
                  CompilerPluginPartialOrderRelation.BEFORE,
                  "plugin.two",
                ),
              ),
          ),
          CompilerPlugin(
            pluginId = "plugin.two",
            classpath = listOf(secondJar),
            rawArguments = emptyList(),
            orderingRequirements = emptySet(),
          ),
        ),
      )

    assertThat(args).containsExactly(
      "-Xplugin=${firstJar.absolutePathString()},${secondJar.absolutePathString()}",
      "-Xcompiler-plugin-order=plugin.one>plugin.two",
    ).inOrder()
  }

  @Test
  fun `filters btapi raw marker plugin`() {
    val args =
      BtapiPluginArguments.toArgumentStrings(
        listOf(
          CompilerPlugin(
            pluginId = "___RAW_PLUGINS_APPLIED___",
            classpath = emptyList(),
            rawArguments = emptyList(),
            orderingRequirements = emptySet(),
          ),
        ),
      )

    assertThat(args).isEmpty()
  }

  @Test
  fun `rejects plugin with empty id`() {
    val exception =
      org.junit.Assert.assertThrows(IllegalStateException::class.java) {
        BtapiPluginArguments.toArgumentStrings(
          listOf(
            CompilerPlugin(
              pluginId = " ",
              classpath = listOf(Path.of("plugin.jar").toAbsolutePath()),
              rawArguments = emptyList(),
              orderingRequirements = emptySet(),
            ),
          ),
        )
      }

    assertThat(exception).hasMessageThat().contains("plugin id is empty")
  }

  @Test
  fun `rejects plugin with empty classpath`() {
    val exception =
      org.junit.Assert.assertThrows(IllegalStateException::class.java) {
        BtapiPluginArguments.toArgumentStrings(
          listOf(
            CompilerPlugin(
              pluginId = "plugin.id",
              classpath = emptyList(),
              rawArguments = emptyList(),
              orderingRequirements = emptySet(),
            ),
          ),
        )
      }

    assertThat(exception).hasMessageThat().contains("has empty classpath")
  }
}
