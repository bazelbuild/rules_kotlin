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
import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer

@RunWith(JUnit4::class)
class KotlinBuilderJvmJdepsTest {
    val ctx = KotlinJvmTestBuilder()

    @After
    fun tearDown() {
      ctx.tearDown()
    }

    @Test
    fun `no dependencies`() {

      val deps = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        c.addSource("AClass.kt",
          """
            package something
            
            class AClass{}
          """)
        c.outputJar()
        c.outputJdeps()
        c.compileKotlin()
      })
      val jdeps = depsProto(deps)

      assertThat(jdeps.dependencyCount).isEqualTo(0)
      assertThat(jdeps.ruleLabel).isEqualTo(deps.label())
  }

  @Test
  fun `java dependencies`() {

    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("AClass.kt",
        """
          package something
          
          class AClass{}
        """)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("AnotherClass.java",
        """
          package something;
          
          class AnotherClass {
            public AClass ref = null;
          }
        """)
      c.outputJar()
      c.compileJava()
      c.outputJavaJdeps()
      c.addDirectDependencies(dependentTarget)
    })
    val jdeps = javaDepsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java constant`() {

    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("Constants.java",
        """
          package something;
          
          public interface Constants {
              int HELLO_CONSTANT = 100;
          }
        """)
      c.outputJar()
      c.outputJavaJdeps()
      c.compileJava()
    })

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("AnotherClass.kt",
        """
          package something.other
          
          import something.Constants
          
          class AnotherClass {
            val ref = Constants.HELLO_CONSTANT
          }
        """)
      c.outputJar()
      c.compileKotlin()
      c.outputJdeps()
      c.addDirectDependencies(dependentTarget)
    })
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `unused dependency listed`() {
    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("AClass.kt",
        """
          package something
          
          class AClass{}
        """)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("HasNoReferenceToDep.kt",
        """
          package something
        """)
      c.outputJar()
      c.compileKotlin()
      c.addDirectDependencies(dependentTarget)
      c.outputJdeps()
    })
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).isEmpty()
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `unused transitive dependency not listed`() {
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

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("HasNoReferenceToDep.kt",
        """
          package something
        """)
      c.outputJar()
      c.compileKotlin()
      c.addDirectDependencies(dependentTarget)
      c.outputJdeps()
    })
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).isEmpty()
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `property dependency`() {
      val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        c.addSource("AClass.kt",
          """
            package something

            class AClass{}
            """)
        c.outputJar()
        c.outputJdeps()
        c.compileKotlin()
      })

      val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        c.addSource("HasPropertyDependency.kt",
          """
            package something
            
            val property2 =  AClass()
          """)
        c.outputJar()
        c.compileKotlin()
        c.addDirectDependencies(dependentTarget)
        c.outputJdeps()
      })
      val jdeps = depsProto(dependingTarget)

      assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

      assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
      assertImplicit(jdeps).isEmpty()
      assertUnused(jdeps).isEmpty()
      assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `inlined constant dependency recorded`() {
    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("ContainsConstant.kt",
        """
          package dependency
            
          object ConstHolder {
            const val CONSTANT_VAL = 42
          }
        """)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("HasPropertyDependency.kt",
        """
            package something
            import dependency.ConstHolder
            val property2 = ConstHolder.CONSTANT_VAL
          """)
      c.outputJar()
      c.compileKotlin()
      c.addDirectDependencies(dependentTarget)
      c.outputJdeps()
    })
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `references function`() {
    val dependentTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("ContainsFunction.kt",
        """
          package dependency
            
          fun someFunction() = 42
        """)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    val dependingTarget = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource("HasFunctionDependency.kt",
        """
            package something
            import dependency.someFunction
            val property2 = someFunction()
          """)
      c.outputJar()
      c.compileKotlin()
      c.addDirectDependencies(dependentTarget)
      c.outputJdeps()
    })
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  private fun depsProto(jdeps: io.bazel.kotlin.builder.Deps.Dep) =
    Deps.Dependencies.parseFrom(BufferedInputStream(Files.newInputStream(Paths.get(jdeps.jdeps()!!))))

  private fun javaDepsProto(jdeps: io.bazel.kotlin.builder.Deps.Dep) =
    Deps.Dependencies.parseFrom(BufferedInputStream(Files.newInputStream(Paths.get(jdeps.javaJdeps()!!))))

  private fun assertExplicit(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.EXPLICIT }.map { it.path }
  )

  private fun assertImplicit(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.IMPLICIT }.map { it.path }
  )

  private fun assertUnused(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.UNUSED }.map { it.path }
  )

  private fun assertIncomplete(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.INCOMPLETE }.map { it.path }
  )
}
