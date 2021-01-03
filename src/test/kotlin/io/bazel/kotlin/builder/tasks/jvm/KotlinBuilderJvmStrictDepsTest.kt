/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder.tasks.jvm;

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.function.Consumer

@RunWith(JUnit4::class)
class KotlinBuilderJvmStrictDepsTest {
  val ctx = KotlinJvmTestBuilder()

  @After
  fun tearDown() {
    ctx.tearDown()
  }

  @Test
  fun `strict dependency violation error`() {
    val transitiveDep = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("TransitiveClass.kt",
        """
          package something
          
          class TransitiveClass{}
        """)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("AClass.kt",
        """
          package something
          
          class AClass{}
        """)
      c.outputJar()
      c.compileKotlin()
      c.addDirectDependencies(transitiveDep)
      c.outputJdeps()
    })


    ctx.runFailingCompileTaskAndValidateOutput(
      {
        ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
            c.addSource("HasTransitiveReference.kt",
              """
          package something
          
          val transitiveReference = TransitiveClass()
        """)
            c.outputJar()
            c.compileKotlin()
            c.addDirectDependencies(dependentTarget)
            c.addTransitiveDependencies(transitiveDep)
            c.outputJdeps()
            c.kotlinStrictDeps("error")
          })
      }
    ) { lines: List<String?> -> assertThat(lines[0]).contains("Strict Deps Violations - please fix") }
  }

}
