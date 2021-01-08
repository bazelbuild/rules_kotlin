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
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import io.bazel.kotlin.builder.tasks.InvocationWorker
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.JavaBuilderFlags
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.KotlinBuilderFlags
import io.bazel.kotlin.builder.tasks.WorkerIO
import io.bazel.kotlin.builder.utils.Flag
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.toList

@RunWith(JUnit4::class)
class KotlinWorkerTest {

  private val wrkDir = Files.createTempDirectory("KotlinBuilderEnvironmentTest")

  class SourceBuilder(private val out: ByteArrayOutputStream = ByteArrayOutputStream()) {
    val ln = "\n".toByteArray(StandardCharsets.UTF_8)
    fun l(l: String) {
      out.write(l.toByteArray(StandardCharsets.UTF_8))
      out.write(ln)
    }

    fun bytes(): ByteArray {
      return out.toByteArray()
    }
  }

  private fun src(path: String, writeLines: (SourceBuilder.() -> Unit)): Path {
    val srcPath = Files.createDirectories(wrkDir.resolve("src")).resolve(path)
    val b = SourceBuilder()
    b.writeLines()
    Files.write(srcPath, b.bytes())

    require(Files.readAllLines(srcPath).isNotEmpty()) {
      "failed to write $srcPath"
    }
    return srcPath
  }

  class ArgsBuilder(val args: MutableMap<Flag, MutableList<String>> = mutableMapOf()) {
    fun flag(flag: Flag, value: String) {
      args[flag] = (args[flag] ?: mutableListOf()).also {
        it.add(value)
      }
    }

    fun flag(flag: Flag, p: Path) {
      flag(flag, p.toString())
    }

    fun cp(flag: Flag, value: String) {
      args[flag] = mutableListOf(
        (args[flag] ?: mutableListOf()).also { it.add(value) }.joinToString(File.pathSeparator))
    }

    fun remove(flag: Flag) {
      args.remove(flag)
    }

    fun source(src: Path) {
      flag(JavaBuilderFlags.SOURCES, src.toString())
    }

    fun list(): List<String> {
      return args.flatMap { entry ->
        entry.value.flatMap { value ->
          listOf(entry.key.flag, value)
        }
      }
    }
  }

  private fun args(
    info: CompilationTaskInfo,
    init: ArgsBuilder.() -> Unit
  ) = with(ArgsBuilder()) {
    flag(JavaBuilderFlags.TARGET_LABEL, info.label)
    flag(KotlinBuilderFlags.MODULE_NAME, info.moduleName)
    flag(KotlinBuilderFlags.API_VERSION, info.toolchainInfo.common.apiVersion)
    flag(KotlinBuilderFlags.LANGUAGE_VERSION, info.toolchainInfo.common.languageVersion)
    flag(JavaBuilderFlags.RULE_KIND, "kt_${info.platform.name}_${info.ruleKind.name}")
    flag(JavaBuilderFlags.CLASSDIR, "kt_classes")
    flag(KotlinBuilderFlags.GENERATED_CLASSDIR, "generated_classes")
    flag(JavaBuilderFlags.TEMPDIR, "tmp")
    flag(JavaBuilderFlags.SOURCEGEN_DIR, "generated_sources")
    flag(KotlinBuilderFlags.STRICT_KOTLIN_DEPS, "off")
    cp(JavaBuilderFlags.CLASSPATH, KotlinJvmTestBuilder.KOTLIN_STDLIB.singleCompileJar())
    cp(JavaBuilderFlags.DIRECT_DEPENDENCIES, "")
    info.passthroughFlagsList.forEach { pf ->
      flag(KotlinBuilderFlags.PASSTHROUGH_FLAGS, pf)
    }
    flag(KotlinBuilderFlags.DEBUG, info.debugList.joinToString(","))
    flag(KotlinBuilderFlags.JVM_TARGET, info.toolchainInfo.jvm.jvmTarget)
    init()
    list()
  }

  @Test
  fun `abi generation`() {

    val builder = KotlinJvmTestBuilder.component().kotlinBuilder()

    val one = src("One.kt") {
      l("package harry.nilsson")
      l("")
      l("class One : Zero(), Imaginary {")
      l("  override val i:Boolean = false")
      l("  override fun isTheLoneliestNumber():String {")
      l("     return \"that you'll ever do\"")
      l("  }")
      l("}")
    }

    val zero = src("Zero.kt") {
      l("package harry.nilsson")
      l("")
      l("abstract class Zero {")
      l("  abstract fun isTheLoneliestNumber():String")
      l("}")
    }

    val imaginary = src("Imaginary.kt") {
      l("package harry.nilsson")
      l("")
      l("interface Imaginary {")
      l("  val i: Boolean get() = true")
      l("}")
    }

    WorkerIO.open().use { io ->
      val worker = InvocationWorker(io, builder)

      val abiJar = out("abi.jar")
      val outputJar = out("output.jar")

      assertThat(
        worker.run(
          args(compilationTaskInfo) {
            flag(KotlinBuilderFlags.ABI_JAR, abiJar)
            flag(JavaBuilderFlags.OUTPUT, outputJar)
            flag(KotlinBuilderFlags.BUILD_KOTLIN, "true")
            flag(JavaBuilderFlags.BUILD_JAVA, "false")
            flag(KotlinBuilderFlags.OUTPUT_JDEPS, "out.jdeps")
            source(one)
            source(zero)
            source(imaginary)
          })).isEqualTo(0)

      assertJarClasses(abiJar).containsExactly(
        "harry/nilsson/Imaginary\$DefaultImpls.class",
        "harry/nilsson/Imaginary.class",
        "harry/nilsson/One.class",
        "harry/nilsson/Zero.class"
      )
    }
  }

  fun assertJarClasses(jar: Path) = assertThat(
    ZipFile(jar.toFile())
      .stream()
      .map(ZipEntry::getName)
      .filter { it.endsWith(".class") }
      .toList()
  )

  @Test
  fun `output directories are different for invocations`() {

    val builder = KotlinJvmTestBuilder.component().kotlinBuilder()

    val one = src("One.kt") {
      l("package harry.nilsson")
      l("")
      l("class One {")
      l("  fun isTheLoneliestNumber():String {")
      l("     return \"that you'll ever do\"")
      l("  }")
      l("}")
    }

    val two = src("Two.kt") {
      l("package harry.nilsson")
      l("class Two {")
      l("  fun canBeAsBadAsOne():String {")
      l("     return \"it is the loneliest number since the number one\"")
      l("  }")
      l("}")
    }

    WorkerIO.open().use { io ->
      val worker = InvocationWorker(io, builder)

      val jarOne = out("one.jar")
      assertThat(worker.run(args(compilationTaskInfo) {
        source(one)
        flag(JavaBuilderFlags.OUTPUT, jarOne.toString())
        flag(KotlinBuilderFlags.OUTPUT_SRCJAR, "$jarOne.srcjar")
        flag(KotlinBuilderFlags.OUTPUT_JDEPS, "out.jdeps")
        flag(KotlinBuilderFlags.BUILD_KOTLIN, "true")
        flag(JavaBuilderFlags.BUILD_JAVA, "false")
      })).isEqualTo(0)

      assertThat(
        ZipFile(jarOne.toFile())
          .stream()
          .map(ZipEntry::getName)
          .filter { it.endsWith(".class") }
          .toList()
      ).containsExactly("harry/nilsson/One.class")

      val jarTwo = out("two.jar")
      assertThat(worker.run(args(compilationTaskInfo) {
        source(two)
        flag(JavaBuilderFlags.OUTPUT, jarTwo.toString())
        flag(KotlinBuilderFlags.OUTPUT_SRCJAR, "$jarTwo.srcjar")
        flag(KotlinBuilderFlags.OUTPUT_JDEPS, "out.jdeps")
        flag(KotlinBuilderFlags.BUILD_KOTLIN, "true")
        flag(JavaBuilderFlags.BUILD_JAVA, "false")
      })).isEqualTo(0)

      assertThat(
        ZipFile(jarTwo.toFile())
          .stream()
          .map(ZipEntry::getName)
          .filter { it.endsWith(".class") }
          .toList()
      ).containsExactly("harry/nilsson/Two.class")
    }
  }

  private fun out(name: String): Path {
    return Files.createDirectories(wrkDir.resolve("out")).resolve(name)
  }

  private val compilationTaskInfo: CompilationTaskInfo
    get() {
      return with(CompilationTaskInfo.newBuilder()) {
        label = "//singing/nilsson:one"
        moduleName = "harry.nilsson"
        platform = Platform.JVM
        ruleKind = RuleKind.LIBRARY
        toolchainInfo = with(toolchainInfoBuilder) {
          common =
            commonBuilder.setApiVersion("1.3").setCoroutines("enabled").setLanguageVersion("1.3")
              .build()
          jvm = jvmBuilder.setJvmTarget("1.8").build()
          build()
        }
        build()
      }
    }
}
