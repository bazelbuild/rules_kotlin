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
package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.Deps.Dep
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * Tests for per-class compilation avoidance feature (class usage tracking).
 */
@RunWith(Parameterized::class)
class KotlinBuilderJvmClassTrackingTest(private val enableK2Compiler: Boolean) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "enableK2Compiler={0}")
    fun data(): Collection<Array<Any>> {
      return listOf(
        arrayOf(true),
        arrayOf(false),
      )
    }
  }

  val ctx = KotlinJvmTestBuilder()

  val TEST_FIXTURES_DEP = Dep.fromLabel(":JdepsParserTestFixtures")

  @Test
  fun `class tracking disabled by default`() {
    val deps = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps")
      c.addSource(
        "AClass.kt",
        """
          package something

          val result = JavaClass.staticMethod()
        """,
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }

    val jdeps = depsProto(deps)

    // Verify jdeps has explicit dependency
    assertThat(jdeps.dependencyCount).isGreaterThan(0)

    // Verify no UsedClass entries when tracking is off
    val explicitDep = jdeps.dependencyList.find { it.kind == Deps.Dependency.Kind.EXPLICIT }
    assertThat(explicitDep).isNotNull()
    assertThat(explicitDep!!.usedClassesCount).isEqualTo(0)
  }

  @Test
  fun `class tracking records used classes with hashes when enabled`() {
    val deps = runJdepsCompileTaskWithClassTracking { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps")
      c.addSource(
        "AClass.kt",
        """
          package something

          val result = JavaClass.staticMethod()
        """,
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }

    val jdeps = depsProto(deps)

    // Verify jdeps has explicit dependency
    assertThat(jdeps.dependencyCount).isGreaterThan(0)

    // Verify UsedClass entries exist when tracking is on
    val explicitDep = jdeps.dependencyList.find { it.kind == Deps.Dependency.Kind.EXPLICIT }
    assertThat(explicitDep).isNotNull()
    assertThat(explicitDep!!.usedClassesCount).isGreaterThan(0)

    // Verify UsedClass has required fields
    val usedClass = explicitDep.usedClassesList.first()
    assertThat(usedClass.fullyQualifiedName).isNotEmpty()
    assertThat(usedClass.internalPath).endsWith(".class")
    assertThat(usedClass.hash.size()).isEqualTo(32) // SHA-256 = 32 bytes
  }

  @Test
  fun `class tracking records multiple used classes`() {
    val deps = runJdepsCompileTaskWithClassTracking { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps")
      c.addSource(
        "MultipleClasses.kt",
        """
          package something

          val result1 = JavaClass.staticMethod()
          val result2 = Constants.HELLO_CONSTANT
        """,
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }

    val jdeps = depsProto(deps)

    // Verify UsedClass entries for multiple classes
    val explicitDep = jdeps.dependencyList.find { it.kind == Deps.Dependency.Kind.EXPLICIT }
    assertThat(explicitDep).isNotNull()

    // Should have tracked multiple classes
    val usedClassNames = explicitDep!!.usedClassesList.map { it.fullyQualifiedName }.toSet()
    assertThat(usedClassNames).contains("something.JavaClass")
    assertThat(usedClassNames).contains("something.Constants")
  }

  @Test
  fun `class tracking includes supertype dependencies`() {
    val baseClass = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//:base")
      c.addSource(
        "Base.kt",
        """
          package something

          open class Base
        """,
      )
    }

    val derivedClass = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//:derived")
      c.addSource(
        "Derived.kt",
        """
          package something

          class Derived : Base()
        """,
      )
      c.addDirectDependencies(baseClass)
    }

    val deps = runJdepsCompileTaskWithClassTracking { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps")
      c.addSource(
        "UsesDerived.kt",
        """
          package something

          val instance = Derived()
        """,
      )
      c.addDirectDependencies(derivedClass)
      c.addTransitiveDependencies(baseClass)
    }

    val jdeps = depsProto(deps)

    // The explicit dep should have Derived class tracked
    val explicitDep = jdeps.dependencyList.find {
      it.kind == Deps.Dependency.Kind.EXPLICIT && it.usedClassesCount > 0
    }
    assertThat(explicitDep).isNotNull()

    val usedClassNames = explicitDep!!.usedClassesList.map { it.fullyQualifiedName }
    assertThat(usedClassNames).contains("something.Derived")
  }

  @Test
  fun `class hashes are deterministic`() {
    // Run the same compilation twice and verify hashes match
    val deps1 = runJdepsCompileTaskWithClassTracking { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps1")
      c.addSource(
        "AClass.kt",
        """
          package something

          val result = JavaClass.staticMethod()
        """,
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }

    val deps2 = runJdepsCompileTaskWithClassTracking { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.setLabel("//deps2")
      c.addSource(
        "BClass.kt",
        """
          package something

          val result = JavaClass.staticMethod()
        """,
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }

    val jdeps1 = depsProto(deps1)
    val jdeps2 = depsProto(deps2)

    // Find JavaClass in both jdeps
    val javaClassHash1 = jdeps1.dependencyList
      .flatMap { it.usedClassesList }
      .find { it.fullyQualifiedName == "something.JavaClass" }
      ?.hash

    val javaClassHash2 = jdeps2.dependencyList
      .flatMap { it.usedClassesList }
      .find { it.fullyQualifiedName == "something.JavaClass" }
      ?.hash

    assertThat(javaClassHash1).isNotNull()
    assertThat(javaClassHash2).isNotNull()
    assertThat(javaClassHash1).isEqualTo(javaClassHash2)
  }

  private fun depsProto(jdeps: Dep) =
    Deps.Dependencies.parseFrom(BufferedInputStream(Files.newInputStream(Paths.get(jdeps.jdeps()!!))))

  private fun runCompileTask(block: (c: KotlinJvmTestBuilder.TaskBuilder) -> Unit): Dep {
    return ctx.runCompileTask(
      Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        if (enableK2Compiler) {
          c.useK2()
        }
        block(c.outputJar().compileKotlin())
      },
    )
  }

  private fun runJdepsCompileTask(block: (c: KotlinJvmTestBuilder.TaskBuilder) -> Unit): Dep {
    return ctx.runCompileTask(
      Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        if (enableK2Compiler) {
          c.useK2()
        }
        block(c.outputJar().outputJdeps().compileKotlin())
      },
    )
  }

  private fun runJdepsCompileTaskWithClassTracking(block: (c: KotlinJvmTestBuilder.TaskBuilder) -> Unit): Dep {
    return ctx.runCompileTask(
      Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        if (enableK2Compiler) {
          c.useK2()
        }
        block(c.outputJar().outputJdeps().trackClassUsage("on").compileKotlin())
      },
    )
  }
}
