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

import java.io.File
import java.io.PrintStream
import java.lang.reflect.Method
import java.net.URLClassLoader

class KotlinToolchain private constructor(
  private val baseJars: List<File>,
  private val buildTools: File,
  val kapt3Plugin: CompilerPlugin,
  val jvmAbiGen: CompilerPlugin,
  val skipCodeGen: CompilerPlugin,
  val jdepsGen: CompilerPlugin,
) {
  companion object {
    internal val NO_ARGS = arrayOf<Any>()

    @JvmStatic
    fun createToolchain(
      kotlinc: File,
      buildTools: File,
      compiler: File,
      jvmAbiGenFile: File,
      skipCodeGenFile: File,
      jdepsGenFile: File,
      kaptFile: File,
      kotlinStdlib: File,
      kotlinReflect: File,
      kotlinxSerializationCoreJvm: File,
      kotlinxSerializationJsonJvm: File,
    ): KotlinToolchain =
      KotlinToolchain(
        mutableListOf<File>().apply {
          add(kotlinc)
          add(compiler)
          // plugins *must* be preloaded. Not doing so causes class conflicts
          // (and a NoClassDef err) in the compiler extension interfaces.
          // This may cause issues in accepting user defined compiler plugins.
          add(jvmAbiGenFile)
          add(skipCodeGenFile)
          add(jdepsGenFile)
          add(kotlinStdlib)
          add(kotlinReflect)
          add(kotlinxSerializationCoreJvm)
          add(kotlinxSerializationJsonJvm)
        },
        buildTools = buildTools,
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
    baseJars: List<File>,
    classLoader: ClassLoader = ClassLoader.getPlatformClassLoader(),
  ): ClassLoader = URLClassLoader(baseJars.map { it.toURI().toURL() }.toTypedArray(), classLoader)

  private val legacyClassLoader by lazy { createClassLoader(baseJars) }
  private val btapiClassLoader by lazy { createClassLoader(baseJars + buildTools) }

  private fun classLoader(useExperimentalBuildToolsAPI: Boolean): ClassLoader =
    if (useExperimentalBuildToolsAPI) {
      btapiClassLoader
    } else {
      legacyClassLoader
    }

  data class CompilerPlugin(
    val jarPath: String,
    val id: String,
  )

  open class KotlincInvoker internal constructor(
    classLoader: ClassLoader,
    clazz: String,
  ) {
    private val compiler: Any
    private val execMethod: Method
    private val getCodeMethod: Method

    init {
      val compilerClass = classLoader.loadClass(clazz)
      val exitCodeClass =
        classLoader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode")

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
      return KotlincInvoker(
        classLoader = toolchain.classLoader(useExperimentalBuildToolsAPI),
        clazz = clazz,
      )
    }
  }
}
