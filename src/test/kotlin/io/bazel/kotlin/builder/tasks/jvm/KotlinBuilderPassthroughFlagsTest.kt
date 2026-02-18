/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder.tasks.jvm

import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.function.Consumer

/**
 * Tests for passthrough flags handling in the Kotlin compiler.
 */
@RunWith(JUnit4::class)
class KotlinBuilderPassthroughFlagsTest {

  val ctx = KotlinJvmTestBuilder()

  @Test
  fun `passthrough flags are applied to compilation`() {
    // Test that passthrough flags work by using -Xsam-conversions flag
    // which affects how SAM conversions are compiled
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.compileKotlin()
      c.outputJar()
      c.outputJdeps()
      c.passthroughFlags("-Xlambdas=class", "-Xsam-conversions=class")
      c.addSource(
        "SamTest.kt",
        """
        package test

        fun interface MyFunction {
          fun invoke(x: Int): Int
        }

        fun useSam(f: MyFunction) = f.invoke(42)

        fun main() {
          useSam { it * 2 }
        }
        """
      )
    })
  }

  @Test
  fun `passthrough flags with -Xno-call-assertions`() {
    // Test that -Xno-call-assertions passthrough flag works
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.compileKotlin()
      c.outputJar()
      c.outputJdeps()
      c.passthroughFlags("-Xno-call-assertions")
      c.addSource(
        "NoCallAssertions.kt",
        """
        package test

        class Example {
          fun greet(name: String): String = "Hello, ${'$'}name!"
        }
        """
      )
    })
  }

  @Test
  fun `passthrough flags with -Xno-param-assertions`() {
    // Test that -Xno-param-assertions passthrough flag works
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.compileKotlin()
      c.outputJar()
      c.outputJdeps()
      c.passthroughFlags("-Xno-param-assertions")
      c.addSource(
        "NoParamAssertions.kt",
        """
        package test

        class Example {
          fun process(input: List<String>): Int = input.size
        }
        """
      )
    })
  }

  @Test
  fun `multiple passthrough flags combined`() {
    // Test multiple passthrough flags together
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.compileKotlin()
      c.outputJar()
      c.outputJdeps()
      c.passthroughFlags(
        "-Xlambdas=class",
        "-Xsam-conversions=class",
        "-Xno-call-assertions",
        "-Xno-param-assertions"
      )
      c.addSource(
        "Combined.kt",
        """
        package test

        fun interface Processor {
          fun process(x: Int): Int
        }

        fun runProcessor(p: Processor) = p.process(10)

        class Example {
          fun execute() = runProcessor { it * 2 }
        }
        """
      )
    })
  }
}
