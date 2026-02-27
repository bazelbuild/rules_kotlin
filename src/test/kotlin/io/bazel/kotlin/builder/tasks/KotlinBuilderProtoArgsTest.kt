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
package io.bazel.kotlin.builder.tasks

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.util.JsonFormat
import io.bazel.kotlin.builder.Deps.Dep
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.KotlinBuilderFlags
import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.worker.Status
import io.bazel.worker.WorkerContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files

@RunWith(JUnit4::class)
class KotlinBuilderProtoArgsTest {
  private val builder = KotlinBuilder(KotlinJvmTaskExecutor())

  private val kotlinStdlib = dep("//kotlin/compiler:kotlin-stdlib")
  private val kotlinReflect = dep("//kotlin/compiler:kotlin-reflect")
  private val kotlinCoroutines = dep("//kotlin/compiler:kotlinx-coroutines-core-jvm")
  private val annotations = dep("//kotlin/compiler:annotations")

  private val buildToolsImpl = dep("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_build_tools_impl")
  private val kotlinCompilerEmbeddable =
    dep("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_compiler_embeddable")
  private val kotlinDaemonClient = dep("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_daemon_client")

  private val jvmAbiGen = dep("//kotlin/compiler:jvm-abi-gen")
  private val skipCodeGen = dep("//src/main/kotlin:skip-code-gen")
  private val kaptEmbeddable = dep("@kotlin_rules_maven//:org_jetbrains_kotlin_kotlin_annotation_processing_embeddable")
  private val jdepsGen = dep("//src/main/kotlin:jdeps-gen")

  @Test
  fun `worker-style request maps plugins payload to compiler args`() {
    val result =
      WorkerContext.run {
        doTask("kotlin-builder-proto-request") { taskContext ->
          val sourcePath =
            taskContext.directory.resolve("src/sample/WithNoArg.kt").also {
              Files.createDirectories(it.parent)
              Files.writeString(
                it,
                """
                package sample

                @Target(AnnotationTarget.CLASS)
                annotation class NoArg

                @NoArg
                class WithNoArg(val value: String)
                """.trimIndent(),
              )
            }
          val outputJar = taskContext.directory.resolve("out.jar")
          val missingPluginJar =
            taskContext.directory.resolve("missing-noarg-plugin.jar").toString()

          val args =
            requestArgs(
              source = sourcePath.toString(),
              output = outputJar.toString(),
              pluginsPayload = noArgPluginsPayload(missingPluginJar),
            )

          if (builder.build(taskContext, args) == 0) {
            Status.SUCCESS
          } else {
            Status.ERROR
          }
        }
      }

    assertThat(result.status).isEqualTo(Status.ERROR)
    assertThat(result.log.toString()).contains("missing-noarg-plugin.jar")
  }

  private fun noArgPluginsPayload(missingPluginJar: String): String {
    val plugin =
      JvmCompilationTask.Inputs.Plugin.newBuilder()
        .setId("org.jetbrains.kotlin.noarg")
        .addClasspath(missingPluginJar)
        .addOptions(
          JvmCompilationTask.Inputs.PluginOption
            .newBuilder()
            .setKey("annotation")
            .setValue("sample.NoArg")
            .build(),
        ).addPhases(JvmCompilationTask.Inputs.PluginPhase.PLUGIN_PHASE_COMPILE)
        .build()

    return JsonFormat.printer().print(
      JvmCompilationTask.Inputs.newBuilder().addPlugins(plugin).build(),
    )
  }

  private fun requestArgs(
    source: String,
    output: String,
    pluginsPayload: String,
  ): List<String> {
    val args = mutableListOf<String>()
    args.addFlag(KotlinBuilderFlags.DEBUG, "trace")
    args.addFlag(KotlinBuilderFlags.TARGET_LABEL, "//sample:proto_test")
    args.addFlag(KotlinBuilderFlags.RULE_KIND, "kt_jvm_library")
    args.addFlag(KotlinBuilderFlags.MODULE_NAME, "proto_test")
    args.addFlag(KotlinBuilderFlags.CLASSPATH, kotlinStdlib)
    args.addFlag(KotlinBuilderFlags.DIRECT_DEPENDENCIES, kotlinStdlib)
    args.addFlag(KotlinBuilderFlags.SOURCES, source)
    args.addFlag(KotlinBuilderFlags.OUTPUT, output)
    args.addFlag(KotlinBuilderFlags.BUILD_KOTLIN, "true")
    args.addFlag(KotlinBuilderFlags.INSTRUMENT_COVERAGE, "false")
    args.addFlag(KotlinBuilderFlags.STRICT_KOTLIN_DEPS, "off")
    args.addFlag(KotlinBuilderFlags.REDUCED_CLASSPATH_MODE, "NONE")
    args.addFlag(KotlinBuilderFlags.API_VERSION, "2.0")
    args.addFlag(KotlinBuilderFlags.LANGUAGE_VERSION, "2.0")
    args.addFlag(KotlinBuilderFlags.JVM_TARGET, "11")
    args.addFlag(KotlinBuilderFlags.PLUGINS_PAYLOAD, pluginsPayload)

    args.addFlag(KotlinBuilderFlags.BUILD_TOOLS_IMPL, buildToolsImpl)
    args.addFlag(KotlinBuilderFlags.KOTLIN_COMPILER_EMBEDDABLE, kotlinCompilerEmbeddable)
    args.addFlag(KotlinBuilderFlags.KOTLIN_DAEMON_CLIENT, kotlinDaemonClient)
    args.addFlag(KotlinBuilderFlags.KOTLIN_STDLIB, kotlinStdlib)
    args.addFlag(KotlinBuilderFlags.KOTLIN_REFLECT, kotlinReflect)
    args.addFlag(KotlinBuilderFlags.KOTLIN_COROUTINES, kotlinCoroutines)
    args.addFlag(KotlinBuilderFlags.ANNOTATIONS, annotations)

    args.addFlag(KotlinBuilderFlags.INTERNAL_JVM_ABI_GEN, jvmAbiGen)
    args.addFlag(KotlinBuilderFlags.INTERNAL_SKIP_CODE_GEN, skipCodeGen)
    args.addFlag(KotlinBuilderFlags.INTERNAL_KAPT, kaptEmbeddable)
    args.addFlag(KotlinBuilderFlags.INTERNAL_JDEPS, jdepsGen)
    return args
  }

  private fun MutableList<String>.addFlag(
    flag: KotlinBuilderFlags,
    vararg values: String,
  ) {
    add(flag.flag)
    addAll(values)
  }

  private fun dep(label: String): String = Dep.fromLabel(label).singleCompileJar()
}
