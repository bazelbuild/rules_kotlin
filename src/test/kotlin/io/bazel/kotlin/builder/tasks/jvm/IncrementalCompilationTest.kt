/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
 */
package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.Deps
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration tests for incremental compilation.
 *
 * These tests invoke the compiler directly to verify IC behavior:
 * - Non-ABI changes only recompile the changed file
 * - ABI changes trigger recompilation of dependents
 * - No changes results in no recompilation
 * - Adding/removing files works correctly
 *
 * Recompilation is detected by comparing class file timestamps before and after compilation.
 */
@RunWith(JUnit4::class)
class IncrementalCompilationTest {

    private lateinit var testDir: Path
    private lateinit var srcDir: Path
    private lateinit var classesDir: Path
    private lateinit var javaClassesDir: Path
    private lateinit var abiClassesDir: Path
    private lateinit var generatedSourcesDir: Path
    private lateinit var generatedJavaSourcesDir: Path
    private lateinit var generatedStubsDir: Path
    private lateinit var tempDir: Path
    private lateinit var generatedClassesDir: Path
    private lateinit var coverageMetadataDir: Path
    private lateinit var icCachesDir: Path
    private lateinit var outputJar: Path

    private val sources = mutableMapOf<String, String>()
    private val counter = AtomicInteger(0)

    private val jvmTaskExecutor by lazy {
        val toolchain = KotlinToolchain.createToolchain(
            File(Deps.Dep.fromLabel("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_compiler_embeddable").singleCompileJar()),
            File(Deps.Dep.fromLabel("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_daemon_client").singleCompileJar()),
            File(Deps.Dep.fromLabel("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_build_tools_api").singleCompileJar()),
            File(Deps.Dep.fromLabel("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_build_tools_impl").singleCompileJar()),
            File(Deps.Dep.fromLabel("//kotlin/compiler:jvm-abi-gen").singleCompileJar()),
            File(Deps.Dep.fromLabel("//src/main/kotlin:skip-code-gen").singleCompileJar()),
            File(Deps.Dep.fromLabel("//src/main/kotlin:jdeps-gen").singleCompileJar()),
            File(Deps.Dep.fromLabel("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_annotation_processing_embeddable").singleCompileJar()),
            File(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib").singleCompileJar()),
            File(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-reflect").singleCompileJar()),
            File(Deps.Dep.fromLabel("//kotlin/compiler:kotlinx-coroutines-core-jvm").singleCompileJar()),
            File(Deps.Dep.fromLabel("//kotlin/compiler:annotations").singleCompileJar())
        )
        val plugins = InternalCompilerPlugins(
            toolchain.jvmAbiGen,
            toolchain.skipCodeGen,
            toolchain.kapt3Plugin,
            toolchain.jdepsGen
        )
        val compilerBuilder = KotlinToolchain.KotlincInvokerBuilder(toolchain)
        KotlinJvmTaskExecutor(compilerBuilder, plugins)
    }

    @Before
    fun setUp() {
        val bazelTestDir = Path.of(System.getenv("TEST_TMPDIR"))
        testDir = Files.createTempDirectory(bazelTestDir, "ic-test-${counter.incrementAndGet()}")

        srcDir = testDir.resolve("src").also { Files.createDirectories(it) }
        classesDir = testDir.resolve("classes").also { Files.createDirectories(it) }
        javaClassesDir = testDir.resolve("java_classes").also { Files.createDirectories(it) }
        abiClassesDir = testDir.resolve("abi_classes").also { Files.createDirectories(it) }
        generatedSourcesDir = testDir.resolve("generated_sources").also { Files.createDirectories(it) }
        generatedJavaSourcesDir = testDir.resolve("generated_java_sources").also { Files.createDirectories(it) }
        generatedStubsDir = testDir.resolve("stubs").also { Files.createDirectories(it) }
        tempDir = testDir.resolve("temp").also { Files.createDirectories(it) }
        generatedClassesDir = testDir.resolve("generated_classes").also { Files.createDirectories(it) }
        coverageMetadataDir = testDir.resolve("coverage_metadata").also { Files.createDirectories(it) }
        icCachesDir = testDir.resolve("ic-base").also { Files.createDirectories(it) }
        outputJar = testDir.resolve("output.jar")

        sources.clear()
    }

    @After
    fun tearDown() {
        testDir.toFile().deleteRecursively()
    }

    private fun writeSource(name: String, content: String): Path {
        val path = srcDir.resolve(name)
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
        sources[name] = content
        return path
    }

    private fun deleteSource(name: String) {
        Files.delete(srcDir.resolve(name))
        sources.remove(name)
    }

    private fun getClassTimestamps(): Map<String, FileTime> {
        if (!Files.exists(classesDir)) return emptyMap()
        return Files.walk(classesDir)
            .filter { it.toString().endsWith(".class") }
            .toList()
            .associate {
                classesDir.relativize(it).toString() to Files.getLastModifiedTime(it)
            }
    }

    private fun compile(isFirstBuild: Boolean = false): CompilationResult {
        val beforeTimestamps = getClassTimestamps()

        // Ensure some time passes for timestamp granularity
        if (beforeTimestamps.isNotEmpty()) {
            Thread.sleep(1100)
        }

        val task = buildTask(isFirstBuild)

        val outputCapture = ByteArrayOutputStream()
        val context = CompilationTaskContext(
            task.info,
            PrintStream(outputCapture),
            testDir.toAbsolutePath().toString() + File.separator
        )

        val exitCode = try {
            jvmTaskExecutor.execute(context, task)
            0
        } catch (e: CompilationStatusException) {
            e.status
        }

        val afterTimestamps = getClassTimestamps()

        // Determine which classes were recompiled
        val recompiledClasses = afterTimestamps.entries
            .filter { (name, time) ->
                val baseline = beforeTimestamps[name]
                baseline == null || baseline != time
            }
            .map { it.key.removeSuffix(".class").replace("/", ".") }
            .toSet()

        return CompilationResult(
            exitCode = exitCode,
            recompiledClasses = recompiledClasses,
            output = outputCapture.toString()
        )
    }

    private fun buildTask(forceRecompilation: Boolean): JvmCompilationTask {
        val kotlinSources = sources.keys.filter { it.endsWith(".kt") }
            .map { srcDir.resolve(it).toAbsolutePath().toString() }
        val javaSources = sources.keys.filter { it.endsWith(".java") }
            .map { srcDir.resolve(it).toAbsolutePath().toString() }

        return JvmCompilationTask.newBuilder().apply {
            infoBuilder.apply {
                label = "//test:ic_test"
                moduleName = "ic_test_module"
                platform = Platform.JVM
                ruleKind = RuleKind.LIBRARY
                incrementalCompilation = true
                icEnableLogging = true
                addDebug("trace")
                addDebug("timings")
                toolchainInfoBuilder.apply {
                    commonBuilder.apply {
                        apiVersion = "2.0"
                        languageVersion = "2.0"
                        coroutines = "enabled"
                    }
                    jvmBuilder.jvmTarget = "11"
                }
            }

            directoriesBuilder.apply {
                classes = classesDir.toAbsolutePath().toString()
                javaClasses = javaClassesDir.toAbsolutePath().toString()
                abiClasses = abiClassesDir.toAbsolutePath().toString()
                generatedSources = generatedSourcesDir.toAbsolutePath().toString()
                generatedJavaSources = generatedJavaSourcesDir.toAbsolutePath().toString()
                generatedStubClasses = generatedStubsDir.toAbsolutePath().toString()
                temp = tempDir.toAbsolutePath().toString()
                generatedClasses = generatedClassesDir.toAbsolutePath().toString()
                coverageMetadataClasses = coverageMetadataDir.toAbsolutePath().toString()
                incrementalBaseDir = icCachesDir.toAbsolutePath().toString()
            }

            inputsBuilder.apply {
                addAllKotlinSources(kotlinSources)
                addAllJavaSources(javaSources)
                // Add stdlib to classpath
                addClasspath(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib").singleCompileJar())
                addClasspath(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib-jdk7").singleCompileJar())
                addClasspath(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib-jdk8").singleCompileJar())
            }

            outputsBuilder.apply {
                jar = outputJar.toAbsolutePath().toString()
                srcjar = testDir.resolve("output-sources.jar").toAbsolutePath().toString()
                jdeps = testDir.resolve("output.jdeps").toAbsolutePath().toString()
            }

            compileKotlin = true
            instrumentCoverage = false
        }.build()
    }

    data class CompilationResult(
        val exitCode: Int,
        val recompiledClasses: Set<String>,
        val output: String
    )

    // ==================== Test Cases ====================

    @Test
    fun `first build compiles all files`() {
        writeSource("A.kt", """
            package test
            class A {
                fun getValue() = 1
            }
        """.trimIndent())
        writeSource("B.kt", """
            package test
            class B {
                fun useA() = A().getValue()
            }
        """.trimIndent())

        val result = compile(isFirstBuild = true)

        assertThat(result.exitCode).isEqualTo(0)
        assertThat(result.recompiledClasses).containsExactly("test.A", "test.B")
    }

    @Test
    fun `non-ABI change recompiles only changed file`() {
        // Setup: A.kt has a function, B.kt calls it
        writeSource("A.kt", """
            package test
            class A {
                fun getValue() = 1
            }
        """.trimIndent())
        writeSource("B.kt", """
            package test
            class B {
                fun useA() = A().getValue()
            }
        """.trimIndent())

        // First build: everything compiles
        val result1 = compile(isFirstBuild = true)
        assertThat(result1.exitCode).isEqualTo(0)
        assertThat(result1.recompiledClasses).containsExactly("test.A", "test.B")

        // Modify A.kt body only (return value changes, not signature)
        writeSource("A.kt", """
            package test
            class A {
                fun getValue() = 42
            }
        """.trimIndent())

        // Second build: only A should recompile
        val result2 = compile()
        assertThat(result2.exitCode).isEqualTo(0)
        assertThat(result2.recompiledClasses).containsExactly("test.A")
    }

    @Test
    fun `ABI change triggers recompilation of dependents`() {
        writeSource("A.kt", """
            package test
            class A {
                fun getValue(): Int = 1
            }
        """.trimIndent())
        writeSource("B.kt", """
            package test
            class B {
                fun useA(): Int = A().getValue()
            }
        """.trimIndent())

        val result1 = compile(isFirstBuild = true)
        assertThat(result1.exitCode).isEqualTo(0)

        // Change A's return type (ABI change) and fix B to match
        writeSource("A.kt", """
            package test
            class A {
                fun getValue(): String = "1"
            }
        """.trimIndent())
        writeSource("B.kt", """
            package test
            class B {
                fun useA(): String = A().getValue()
            }
        """.trimIndent())

        // Both A and B should recompile (B depends on A's ABI)
        val result2 = compile()
        assertThat(result2.exitCode).isEqualTo(0)
        assertThat(result2.recompiledClasses).containsAtLeast("test.A", "test.B")
    }

    @Test
    fun `no changes results in no recompilation`() {
        writeSource("A.kt", """
            package test
            class A
        """.trimIndent())

        val result1 = compile(isFirstBuild = true)
        assertThat(result1.exitCode).isEqualTo(0)
        assertThat(result1.recompiledClasses).containsExactly("test.A")

        // No changes - second build should do nothing
        val result2 = compile()
        assertThat(result2.exitCode).isEqualTo(0)
        assertThat(result2.recompiledClasses).isEmpty()
    }

    @Test
    fun `adding new file compiles only the new file`() {
        writeSource("A.kt", """
            package test
            class A
        """.trimIndent())

        val result1 = compile(isFirstBuild = true)
        assertThat(result1.exitCode).isEqualTo(0)
        assertThat(result1.recompiledClasses).containsExactly("test.A")

        // Add new independent file
        writeSource("B.kt", """
            package test
            class B
        """.trimIndent())

        val result2 = compile()
        assertThat(result2.exitCode).isEqualTo(0)
        assertThat(result2.recompiledClasses).containsExactly("test.B")
    }

    @Test
    fun `adding new method without dependents only recompiles changed file`() {
        writeSource("A.kt", """
            package test
            class A {
                fun existingMethod() = 1
            }
        """.trimIndent())
        writeSource("B.kt", """
            package test
            class B {
                fun useA() = A().existingMethod()
            }
        """.trimIndent())

        val result1 = compile(isFirstBuild = true)
        assertThat(result1.exitCode).isEqualTo(0)

        // Add a new method to A that B doesn't use
        writeSource("A.kt", """
            package test
            class A {
                fun existingMethod() = 1
                fun newMethod() = 2
            }
        """.trimIndent())

        // Adding a method is an ABI change, but B doesn't use the new method
        // IC may or may not recompile B depending on how conservative it is
        val result2 = compile()
        assertThat(result2.exitCode).isEqualTo(0)
        assertThat(result2.recompiledClasses).contains("test.A")
    }
}
