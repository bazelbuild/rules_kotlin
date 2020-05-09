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
import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import io.bazel.kotlin.builder.tasks.InvocationWorker
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.JavaBuilderFlags
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.KotlinBuilderFlags
import io.bazel.kotlin.builder.tasks.WorkerIO
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
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

  private fun args(src: Path, jar: Path, info: CompilationTaskInfo) = listOf(
    JavaBuilderFlags.TARGET_LABEL.flag, info.label,
    KotlinBuilderFlags.MODULE_NAME.flag, info.moduleName,
    KotlinBuilderFlags.API_VERSION.flag, info.toolchainInfo.common.apiVersion,
    KotlinBuilderFlags.LANGUAGE_VERSION.flag, info.toolchainInfo.common.languageVersion,
    JavaBuilderFlags.RULE_KIND.flag, "kt_${info.platform.name}_${info.ruleKind.name}",
    JavaBuilderFlags.OUTPUT.flag, jar.toString(),
    KotlinBuilderFlags.OUTPUT_SRCJAR.flag, "$jar.srcjar",
    KotlinBuilderFlags.OUTPUT_JDEPS.flag, "out.jdeps",
    JavaBuilderFlags.CLASSDIR.flag, "kt_classes",
    KotlinBuilderFlags.GENERATED_CLASSDIR.flag, "generated_classes",
    JavaBuilderFlags.TEMPDIR.flag, "tmp",
    JavaBuilderFlags.SOURCEGEN_DIR.flag, "generated_sources",
    JavaBuilderFlags.CLASSPATH.flag, KotlinJvmTestBuilder.KOTLIN_STDLIB.singleCompileJar(),
    JavaBuilderFlags.SOURCES.flag, src.toString(),
    KotlinBuilderFlags.PASSTHROUGH_FLAGS.flag, info.passthroughFlags,
    KotlinBuilderFlags.DEBUG.flag, info.debugList.joinToString(","),
    KotlinBuilderFlags.JVM_TARGET.flag, info.toolchainInfo.jvm.jvmTarget
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
    val jarOne = out("one.jar")

    WorkerIO.open().use { io ->
      val worker = InvocationWorker(io, builder)

      assertThat(worker.run(args(one, jarOne, compilationTaskInfo))).isEqualTo(0)

      assertWithMessage(String(io.execution.toByteArray(), StandardCharsets.UTF_8)).that(
        ZipFile(jarOne.toFile())
          .stream()
          .map(ZipEntry::getName)
          .filter { it.endsWith(".class") }
          .toList()
      ).containsExactly("harry/nilsson/One.class")

      val jarTwo = out("two.jar")
      assertThat(worker.run(args(two, jarTwo, compilationTaskInfo))).isEqualTo(0)
      assertWithMessage(String(io.execution.toByteArray(), StandardCharsets.UTF_8)).that(
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
