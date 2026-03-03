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

import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.verified
import io.bazel.kotlin.builder.utils.verifiedPath
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

class KotlinToolchain private constructor(
  private val baseJars: List<File>,
  val kapt3Plugin: CompilerPlugin,
  val jvmAbiGen: CompilerPlugin,
  val skipCodeGen: CompilerPlugin,
  val jdepsGen: CompilerPlugin,
) {
  companion object {
    private fun resolveFromProperty(propertyKey: String): Path {
      val propertyValue =
        System.getProperty(propertyKey)
          ?: throw FileNotFoundException("missing toolchain property: $propertyKey")
      return Paths.get(propertyValue).toAbsolutePath().verifiedPath()
    }

    private val JVM_ABI_PLUGIN by lazy {
      resolveFromProperty("..kotlin.compiler.jvm-abi-gen")
    }

    private val KAPT_PLUGIN by lazy {
      resolveFromProperty("..kotlin.compiler.kotlin-annotation-processing")
    }

    private val COMPILER by lazy {
      resolveFromProperty("..src.main.kotlin.compiler")
    }

    private val SKIP_CODE_GEN_PLUGIN by lazy {
      resolveFromProperty("..src.main.kotlin.skip-code-gen")
    }

    private val JDEPS_GEN_PLUGIN by lazy {
      resolveFromProperty("..src.main.kotlin.jdeps-gen")
    }

    private val KOTLINC by lazy {
      resolveFromProperty("..kotlin.compiler.kotlin-compiler")
    }

    private val KOTLIN_REFLECT by lazy {
      resolveFromProperty("..kotlin.compiler.kotlin-reflect")
    }

    private val KOTLIN_STDLIB by lazy {
      resolveFromProperty("..kotlin.compiler.kotlin-stdlib")
    }

    private val KOTLINX_SERIALIZATION_CORE_JVM by lazy {
      resolveFromProperty("..kotlin.compiler.kotlinx-serialization-core-jvm")
    }

    private val KOTLINX_SERIALIZATION_JSON by lazy {
      resolveFromProperty("..kotlin.compiler.kotlinx-serialization-json-jvm")
    }

    private val KOTLINX_SERIALIZATION_JSON_JVM by lazy {
      resolveFromProperty("..kotlin.compiler.kotlinx-serialization-json-jvm")
    }

    private val BUILD_TOOLS_API by lazy {
      resolveFromProperty("..kotlin.compiler.kotlin-build-tools-impl")
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
        kotlinc = KOTLINC.verified().absoluteFile,
        buildTools = BUILD_TOOLS_API.verified().absoluteFile,
        compiler = COMPILER.verified().absoluteFile,
        jvmAbiGenFile = JVM_ABI_PLUGIN.verified().absoluteFile,
        skipCodeGenFile = SKIP_CODE_GEN_PLUGIN.verified().absoluteFile,
        jdepsGenFile = JDEPS_GEN_PLUGIN.verified().absoluteFile,
        kaptFile = KAPT_PLUGIN.verified().absoluteFile,
        extraClasspath =
          listOf(
            KOTLIN_STDLIB.toFile(),
            KOTLIN_REFLECT.toFile(),
            KOTLINX_SERIALIZATION_CORE_JVM.toFile(),
            KOTLINX_SERIALIZATION_JSON.toFile(),
            KOTLINX_SERIALIZATION_JSON_JVM.toFile(),
          ),
      )

    @JvmStatic
    fun createToolchain(
      kotlinc: File,
      buildTools: File,
      compiler: File,
      jvmAbiGenFile: File,
      skipCodeGenFile: File,
      jdepsGenFile: File,
      kaptFile: File,
      kotlinxSerializationCoreJvm: File,
      kotlinxSerializationJson: File,
      kotlinxSerializationJsonJvm: File,
    ): KotlinToolchain =
      createToolchain(
        kotlinc = kotlinc,
        buildTools = buildTools,
        compiler = compiler,
        jvmAbiGenFile = jvmAbiGenFile,
        skipCodeGenFile = skipCodeGenFile,
        jdepsGenFile = jdepsGenFile,
        kaptFile = kaptFile,
        extraClasspath =
          listOf(
            kotlinxSerializationCoreJvm,
            kotlinxSerializationJson,
            kotlinxSerializationJsonJvm,
          ),
      )

    @JvmStatic
    fun createToolchain(
      kotlinc: File,
      buildTools: File,
      compiler: File,
      jvmAbiGenFile: File,
      skipCodeGenFile: File,
      jdepsGenFile: File,
      kaptFile: File,
      extraClasspath: List<File>,
    ): KotlinToolchain =
      KotlinToolchain(
        mutableListOf<File>().apply {
          add(kotlinc)
          add(compiler)
          add(buildTools)
          // plugins *must* be preloaded. Not doing so causes class conflicts
          // (and a NoClassDef err) in the compiler extension interfaces.
          // This may cause issues in accepting user defined compiler plugins.
          add(jvmAbiGenFile)
          add(skipCodeGenFile)
          add(jdepsGenFile)
          addAll(extraClasspath)
        },
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
      )
  }

  private fun createClassLoader(
    javaHome: Path,
    baseJars: List<File>,
    classLoader: ClassLoader = ClassLoader.getPlatformClassLoader(),
  ): ClassLoader {
    val jars =
      mutableListOf<File>().also {
        it += baseJars
        if (!isJdk9OrNewer) {
          it += javaHome.resolveVerified("lib", "tools.jar")
        }
      }
    return URLClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), classLoader)
  }

  val classLoader by lazy {
    createClassLoader(
      JAVA_HOME,
      baseJars,
    )
  }

  fun toolchainWithReflect(kotlinReflect: File? = null): KotlinToolchain =
    KotlinToolchain(
      baseJars + listOf(kotlinReflect ?: KOTLIN_REFLECT.toFile()),
      kapt3Plugin,
      jvmAbiGen,
      skipCodeGen,
      jdepsGen,
    )

  data class CompilerPlugin(
    val jarPath: String,
    val id: String,
  )

  open class KotlincInvoker internal constructor(
    toolchain: KotlinToolchain,
    clazz: String,
  ) {
    private val compiler: Any
    private val execMethod: Method
    private val getCodeMethod: Method

    init {
      val compilerClass = toolchain.classLoader.loadClass(clazz)
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

  class KotlincInvokerBuilder(
    private val toolchain: KotlinToolchain,
  ) {
    fun build(useExperimentalBuildToolsAPI: Boolean): KotlincInvoker {
      val clazz =
        if (useExperimentalBuildToolsAPI) {
          "io.bazel.kotlin.compiler.BuildToolsAPICompiler"
        } else {
          "io.bazel.kotlin.compiler.BazelK2JVMCompiler"
        }
      return KotlincInvoker(toolchain = toolchain, clazz = clazz)
    }
  }
}
