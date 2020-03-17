package io.bazel.kotlin.builder.tasks.jvm

import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinJvmTaskExecutorTest {

  private val ctx = KotlinJvmTestBuilder()

  @Test
  fun testSimpleGeneratedNonJvmSourcesIgnored() {
    ctx.resetForNext()
    ctx.writeGeneratedSourceFile(
      "AGenClass.kt",
      arrayOf("package something.gen;", "class AGenClass{}")
    )
    ctx.writeGeneratedSourceFile(
      "AnotherGenClass.java",
      arrayOf("package something.gen;", "class AnotherGenClass{}")
    )
    ctx.writeGeneratedSourceFile(
      "ignore-me.txt",
      arrayOf("contents do not matter")
    )
    ctx.writeSourceFile(
      "ignore-me-regular-src.kt",
      arrayOf("contents do not matter")
    )
    ctx.writeSourceFile(
      "ignore-me-another-regular-src.java",
      arrayOf("contents do not matter")
    )
    val compileTask = ctx.buildTask()

    assertFalse(compileTask.hasInputs())

    val expandedCompileTask = compileTask.expandWithGeneratedSources()

    assertFalse(compileTask.hasInputs())

    assertTrue(expandedCompileTask.hasInputs())
    assertNotNull(expandedCompileTask.inputs.javaSourcesList.find { path ->
      path.endsWith("a_test_1/generated_sources/AnotherGenClass.java")
    })
    assertEquals(expandedCompileTask.inputs.javaSourcesCount, 1)
    assertNotNull(expandedCompileTask.inputs.kotlinSourcesList.find { path ->
      path.endsWith("a_test_1/generated_sources/AGenClass.kt")
    })
    assertEquals(expandedCompileTask.inputs.kotlinSourcesCount, 1)
  }
}
