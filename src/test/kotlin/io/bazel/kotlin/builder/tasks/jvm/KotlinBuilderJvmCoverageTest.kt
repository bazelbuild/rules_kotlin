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

import io.bazel.kotlin.builder.DirectoryType
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.function.Consumer

@RunWith(JUnit4::class)
class KotlinBuilderJvmCoverageTest {
  val ctx = KotlinJvmTestBuilder()

  @Test
  fun `generates coverage metadata`() {
    val deps = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("JavaClass.java",
        """
          package something;
          
          class JavaClass {
          }
        """)
      c.addSource("KotlinClass.kt",
        """
            package something
            
            class KotlinClass{}
          """)
      c.outputJar()
      c.compileKotlin()
      c.compileJava()
      c.coverage()
      c.outputJdeps()
      c.outputJavaJdeps()
    })

    ctx.assertFilesExist(DirectoryType.COVERAGE_METADATA, "something/JavaClass.class.uninstrumented")
    ctx.assertFilesExist(DirectoryType.COVERAGE_METADATA, "something/KotlinClass.class.uninstrumented")
    ctx.assertFilesExist(DirectoryType.COVERAGE_METADATA, "jar_file.jar-paths-for-coverage.txt")
  }
}
