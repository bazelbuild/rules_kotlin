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
import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.verified
import io.bazel.kotlin.builder.utils.verifiedPath
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.io.PrintStream
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

class KotlinToolchain private constructor(
  private val baseJars: List<File>,
  val kapt3Plugin: CompilerPlugin,
  val jvmAbiGen: CompilerPlugin,
  val skipCodeGen: CompilerPlugin,
  val jdepsGen: CompilerPlugin,
  internal val buildToolsImplJar: File,
  internal val compilerJar: File,
  internal val kotlinCompilerEmbeddableJar: File,
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
          "@rules_kotlin..kotlin.compiler.kotlin-reflect",
        ).toPath()
    }

    internal val KOTLIN_STDLIB by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin..kotlin.compiler.kotlin-stdlib",
        ).toPath()
    }

    internal val KOTLIN_COROUTINES by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin..kotlin.compiler.kotlin-coroutines",
        ).toPath()
    }

    private val KOTLINX_SERIALIZATION_CORE_JVM by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlinx...serialization-core-jvm",
        ).toPath()
    }

    private val KOTLINX_SERIALIZATION_JSON by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlinx...serialization-json",
        ).toPath()
    }

    private val KOTLINX_SERIALIZATION_JSON_JVM by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlinx...serialization-json-jvm",
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

    private val JAVA_HOME by lazy {
      FileSystems
        .getDefault()
        .getPath(System.getProperty("java.home"))
        .let { path ->
          path.takeIf { !it.endsWith(Paths.get("jre")) } ?: path.parent
        }.verifiedPath()
    }

    internal val NO_ARGS = arrayOf<Any>()

    private val isJdk9OrNewer = !System.getProperty("java.version").startsWith("1.")

    @JvmStatic
    fun createToolchain(): KotlinToolchain =
      createToolchain(
        KOTLIN_COMPILER_EMBEDDABLE.verified().absoluteFile,
        BUILD_TOOLS_API.verified().absoluteFile,
        BUILD_TOOLS_IMPL.verified().absoluteFile,
        COMPILER.verified().absoluteFile,
        JVM_ABI_PLUGIN.verified().absoluteFile,
        SKIP_CODE_GEN_PLUGIN.verified().absoluteFile,
        JDEPS_GEN_PLUGIN.verified().absoluteFile,
        KAPT_PLUGIN.verified().absoluteFile,
        KOTLINX_SERIALIZATION_CORE_JVM.toFile(),
        KOTLINX_SERIALIZATION_JSON.toFile(),
        KOTLINX_SERIALIZATION_JSON_JVM.toFile(),
        KOTLIN_STDLIB.verified().absoluteFile,
        KOTLIN_REFLECT.verified().absoluteFile,
        KOTLIN_COROUTINES.verified().absoluteFile,
        ANNOTATIONS.verified().absoluteFile,
      )

    @JvmStatic
    fun createToolchain(
      kotlinCompilerEmbeddable: File,
      buildToolsApi: File,
      buildToolsImpl: File,
      compiler: File,
      jvmAbiGenFile: File,
      skipCodeGenFile: File,
      jdepsGenFile: File,
      kaptFile: File,
      kotlinxSerializationCoreJvm: File,
      kotlinxSerializationJson: File,
      kotlinxSerializationJsonJvm: File,
      kotlinStdlib: File,
      kotlinReflect: File,
      kotlinCoroutines: File,
      annotations: File,
    ): KotlinToolchain =
      KotlinToolchain(
        listOf(
          kotlinCompilerEmbeddable,
          compiler,
          buildToolsApi,
          buildToolsImpl,
          // plugins *must* be preloaded. Not doing so causes class conflicts
          // (and a NoClassDef err) in the compiler extension interfaces.
          // This may cause issues in accepting user defined compiler plugins.
          jvmAbiGenFile,
          skipCodeGenFile,
          jdepsGenFile,
          kotlinxSerializationCoreJvm,
          kotlinxSerializationJson,
          kotlinxSerializationJsonJvm,
        ),
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
        kotlinStdlibJar = kotlinStdlib,
        kotlinReflectJar = kotlinReflect,
        kotlinCoroutinesJar = kotlinCoroutines,
        annotationsJar = annotations,
      )
  }

  private fun createClassLoader(
    javaHome: Path,
    baseJars: List<File>,
    classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
  ): ClassLoader =
    runCatching {
      ClassPreloadingUtils.preloadClasses(
        mutableListOf<File>().also {
          it += baseJars
          if (!isJdk9OrNewer) {
            it += javaHome.resolveVerified("lib", "tools.jar")
          }
        },
        Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
        classLoader,
        null,
      )
    }.onFailure {
      throw RuntimeException("$javaHome, $baseJars", it)
    }.getOrThrow()

  val classLoader by lazy {
    createClassLoader(
      JAVA_HOME,
      baseJars,
    )
  }

  data class CompilerPlugin(
    val jarPath: String,
    val id: String,
  )

  open class KotlincInvoker internal constructor(
    toolchain: KotlinToolchain,
  ) {
    private val compiler: Any
    private val execMethod: Method
    private val getCodeMethod: Method

    init {
      val compilerClass = toolchain.classLoader.loadClass("io.bazel.kotlin.compiler.BuildToolsAPICompiler")
      val exitCodeClass =
        toolchain.classLoader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode")

      compiler = compilerClass.getConstructor().newInstance()
      execMethod =
        compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
      getCodeMethod = exitCodeClass.getMethod("getCode")
    }

    // Kotlin error codes:
    // 1 is a standard compilation error
    // 2 is an internal error
    // 3 is the script execution error
    fun compile(
      args: Array<String>,
      out: PrintStream,
    ): Int {
      val exitCodeInstance = execMethod.invoke(compiler, out, args)
      return getCodeMethod.invoke(exitCodeInstance, *NO_ARGS) as Int
    }
  }

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
      System.err.println("DEBUG: generate called with inputJar=$inputJar, outputSnapshot=$outputSnapshot, granularity=$granularity")
//      generateMethod.invoke(null, inputJar, outputSnapshot, granularity)
    }
  }

  @Singleton
  class KotlincInvokerBuilder
    @Inject
    constructor(
      private val toolchain: KotlinToolchain,
    ) {
      /** Build-tools-impl JAR for BTAPI */
      val buildToolsImplJar: File get() = toolchain.buildToolsImplJar

      /** Compiler JAR */
      val compilerJar: File get() = toolchain.compilerJar

      /** Kotlin compiler embeddable JAR for BTAPI classloader */
      val kotlinCompilerEmbeddableJar: File get() = toolchain.kotlinCompilerEmbeddableJar

      /** Kotlin stdlib JAR for BTAPI classloader */
      val kotlinStdlibJar: File get() = toolchain.kotlinStdlibJar

      val kotlinReflectJar: File get() = toolchain.kotlinReflectJar

      val kotlinCoroutinesJar: File get() = toolchain.kotlinCoroutinesJar

      val annotationsJar: File get() = toolchain.annotationsJar

      /** All base JARs needed for the compiler classloader (includes kotlin-stdlib, etc.) */
      val baseJars: List<File> get() = toolchain.baseJars

      fun build(): KotlincInvoker =
        KotlincInvoker(
          toolchain = toolchain,
        )

      fun buildSnapshotInvoker(): ClasspathSnapshotInvoker =
        ClasspathSnapshotInvoker(
          toolchain.buildToolsImplJar,
          toolchain.compilerJar,
        )
    }
}
