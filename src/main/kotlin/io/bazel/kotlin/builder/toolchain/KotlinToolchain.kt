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
package io.bazel.kotlin.builder.toolchain

import io.bazel.kotlin.builder.utils.BazelRunFiles
import io.bazel.kotlin.builder.utils.verified
import io.bazel.kotlin.builder.utils.verifiedPath
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Paths

class KotlinToolchain private constructor(
  val kapt3Plugin: CompilerPlugin,
  val jvmAbiGen: CompilerPlugin,
  val skipCodeGen: CompilerPlugin,
  val jdepsGen: CompilerPlugin,
  internal val buildToolsImplJar: File,
  internal val compilerJar: File,
  internal val kotlinCompilerEmbeddableJar: File,
  internal val kotlinDaemonEmbeddableJar: File,
  internal val kotlinStdlibJar: File,
  internal val kotlinReflectJar: File,
  internal val kotlinCoroutinesJar: File,
  internal val annotationsJar: File,
) {
  companion object {
    private val JVM_ABI_PLUGIN by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...jvm-abi-gen",
        ).toPath()
    }

    private val KAPT_PLUGIN by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...kapt",
        ).toPath()
    }

    internal val COMPILER by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...compiler",
        ).toPath()
    }

    private val SKIP_CODE_GEN_PLUGIN by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...skip-code-gen",
        ).toPath()
    }

    private val JDEPS_GEN_PLUGIN by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...jdeps-gen",
        ).toPath()
    }

    private val KOTLIN_REFLECT by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...kotlin.compiler.kotlin-reflect",
        ).toPath()
    }

    internal val KOTLIN_STDLIB by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...kotlin.compiler.kotlin-stdlib",
        ).toPath()
    }

    internal val KOTLIN_COROUTINES by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...kotlin.compiler.kotlin-coroutines",
        ).toPath()
    }

    internal val BUILD_TOOLS_API by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...build-tools-api",
        ).toPath()
    }

    internal val BUILD_TOOLS_IMPL by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...build-tools-impl",
        ).toPath()
    }

    private val ANNOTATIONS by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin...kotlin.compiler.annotations",
        ).toPath()
    }

    internal val KOTLIN_COMPILER_EMBEDDABLE by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...kotlin-compiler-embeddable",
        ).toPath()
    }

    internal val KOTLIN_DAEMON_EMBEDDABLE by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@kotlin_rules_maven...kotlin-daemon-client",
        ).toPath()
    }

    @JvmStatic
    fun createToolchain(): KotlinToolchain =
      createToolchain(
        KOTLIN_COMPILER_EMBEDDABLE.verified().absoluteFile,
        KOTLIN_DAEMON_EMBEDDABLE.verified().absoluteFile,
        BUILD_TOOLS_API.verified().absoluteFile,
        BUILD_TOOLS_IMPL.verified().absoluteFile,
        COMPILER.verified().absoluteFile,
        JVM_ABI_PLUGIN.verified().absoluteFile,
        SKIP_CODE_GEN_PLUGIN.verified().absoluteFile,
        JDEPS_GEN_PLUGIN.verified().absoluteFile,
        KAPT_PLUGIN.verified().absoluteFile,
        KOTLIN_STDLIB.verified().absoluteFile,
        KOTLIN_REFLECT.verified().absoluteFile,
        KOTLIN_COROUTINES.verified().absoluteFile,
        ANNOTATIONS.verified().absoluteFile,
      )

    @JvmStatic
    fun createToolchain(
      kotlinCompilerEmbeddable: File,
      kotlinDaemonEmbeddable: File,
      buildToolsApi: File,
      buildToolsImpl: File,
      compiler: File,
      jvmAbiGenFile: File,
      skipCodeGenFile: File,
      jdepsGenFile: File,
      kaptFile: File,
      kotlinStdlib: File,
      kotlinReflect: File,
      kotlinCoroutines: File,
      annotations: File,
    ): KotlinToolchain =
      KotlinToolchain(
        jvmAbiGen =
          CompilerPlugin(
            jvmAbiGenFile.path,
            "org.jetbrains.kotlin.jvm.abi",
          ),
        skipCodeGen =
          CompilerPlugin(
            skipCodeGenFile.path,
            "io.bazel.kotlin.plugin.SkipCodeGen",
          ),
        jdepsGen =
          CompilerPlugin(
            jdepsGenFile.path,
            "io.bazel.kotlin.plugin.jdeps.JDepsGen",
          ),
        kapt3Plugin =
          CompilerPlugin(
            kaptFile.path,
            "org.jetbrains.kotlin.kapt3",
          ),
        buildToolsImplJar = buildToolsImpl,
        compilerJar = compiler,
        kotlinCompilerEmbeddableJar = kotlinCompilerEmbeddable,
        kotlinDaemonEmbeddableJar = kotlinDaemonEmbeddable,
        kotlinStdlibJar = kotlinStdlib,
        kotlinReflectJar = kotlinReflect,
        kotlinCoroutinesJar = kotlinCoroutines,
        annotationsJar = annotations,
      )
  }

  data class CompilerPlugin(
    val jarPath: String,
    val id: String,
  )

  @OptIn(ExperimentalBuildToolsApi::class)
  class ClasspathSnapshotInvoker internal constructor(
    buildToolsImplJar: File,
    compilerJar: File,
  ) {
    private val generateMethod: Method

    init {
      val urls = listOf(compilerJar, buildToolsImplJar).map { it.toURI().toURL() }.toTypedArray()

      val isolatedClassLoader =
        URLClassLoader(
          urls,
          SharedApiClassesClassLoader(),
        )

      val clazz =
        isolatedClassLoader.loadClass(
          "io.bazel.kotlin.compiler.ClasspathSnapshotGenerator",
        )
      generateMethod =
        clazz.getMethod(
          "generate",
          String::class.java,
          String::class.java,
          String::class.java,
        )
    }

    fun generate(
      inputJar: String,
      outputSnapshot: String,
      granularity: String,
    ) {
//      generateMethod.invoke(null, inputJar, outputSnapshot, granularity)
    }
  }

  class KotlincInvokerBuilder(
    private val toolchain: KotlinToolchain,
  ) {
    fun buildSnapshotInvoker(): ClasspathSnapshotInvoker =
      ClasspathSnapshotInvoker(
        toolchain.buildToolsImplJar,
        toolchain.compilerJar,
      )

    @OptIn(ExperimentalBuildToolsApi::class)
    fun createBtapiToolchains(): KotlinToolchains {
      val classpath = mutableListOf<File>()
      classpath.add(toolchain.buildToolsImplJar)
      classpath.add(toolchain.kotlinDaemonEmbeddableJar)
      classpath.add(toolchain.kotlinCompilerEmbeddableJar)
      classpath.add(toolchain.kotlinStdlibJar)
      classpath.add(toolchain.kotlinReflectJar)
      classpath.add(toolchain.kotlinCoroutinesJar)
      classpath.add(toolchain.annotationsJar)

      val urls = classpath.map { it.toURI().toURL() }.toTypedArray()

      // SharedApiClassesClassLoader ensures API classes (from build-tools-api)
      // are shared between the caller and the loaded implementation
      val classLoader = URLClassLoader(urls, SharedApiClassesClassLoader())

      return KotlinToolchains.loadImplementation(classLoader)
    }
  }
}
