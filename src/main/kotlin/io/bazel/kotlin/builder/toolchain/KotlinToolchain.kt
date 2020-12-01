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
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.lang.reflect.Method
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

class KotlinToolchain private constructor(
  val kotlinHome: Path,
  val classLoader: ClassLoader,
  val kotlinStandardLibraries: List<String> = listOf(
    "kotlin-stdlib.jar",
    "kotlin-stdlib-jdk7.jar",
    "kotlin-stdlib-jdk8.jar"
  ),
  val kapt3Plugin: CompilerPlugin = CompilerPlugin(
    kotlinHome.resolveVerified("lib", "kotlin-annotation-processing.jar").absolutePath,
    "org.jetbrains.kotlin.kapt3"
  ),
  val jvmAbiGen: CompilerPlugin,
  val skipCodeGen: CompilerPlugin,
  val jdepsGen: CompilerPlugin
) {

  companion object {
    // TODO(issue/432): Remove this gross hack and pass the file locations on the command line.
    private var RULES_REPOSITORY_NAME =
      System.getenv("TEST_WORKSPACE")?.takeIf { it.isNotBlank() }
      ?: System.getenv("REPOSITORY_NAME")?.takeIf { it.isNotBlank() }
   //   ?: System.getProperty("TEST_WORKSPACE")?.takeIf { it.isNotBlank() }
      ?: error("Unable to determine rules_kotlin repository name.\nenv:${System.getenv()}\nproperties:${System.getProperties()}")

    private val DEFAULT_JVM_ABI_PATH = BazelRunFiles.resolveVerified(
      "external", "com_github_jetbrains_kotlin", "lib", "jvm-abi-gen.jar"
    ).toPath()

    private val COMPILER = BazelRunFiles.resolveVerified(
      RULES_REPOSITORY_NAME,
      "src", "main", "kotlin", "io", "bazel", "kotlin", "compiler",
      "compiler.jar").toPath()

    private val SKIP_CODE_GEN_PLUGIN = BazelRunFiles.resolveVerified(
      RULES_REPOSITORY_NAME,
      "src", "main", "kotlin",
      "skip-code-gen.jar").toPath()

    private val JDEPS_GEN_PLUGIN = BazelRunFiles.resolveVerified(
      RULES_REPOSITORY_NAME,
      "src", "main", "kotlin",
      "jdeps-gen.jar").toPath()

    internal val NO_ARGS = arrayOf<Any>()

    private val isJdk9OrNewer = !System.getProperty("java.version").startsWith("1.")

    private fun createClassLoader(javaHome: Path, baseJars: List<File>): ClassLoader =
      ClassPreloadingUtils.preloadClasses(
        mutableListOf<File>().also {
          it += baseJars
          if (!isJdk9OrNewer) {
            it += javaHome.resolveVerified("lib", "tools.jar")
          }
        },
        Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
        ClassLoader.getSystemClassLoader(),
        null
      )

    @JvmStatic
    fun createToolchain(): KotlinToolchain {
      return createToolchain(DEFAULT_JVM_ABI_PATH)
    }

    @JvmStatic
    fun createToolchain(jvmAbiGenPath: Path): KotlinToolchain {
      val df = FileSystems.getDefault()
      val javaHome = df.getPath(System.getProperty("java.home")).let { path ->
        path.takeIf { !it.endsWith(Paths.get("jre")) } ?: path.parent
      }.verifiedPath()

      val skipCodeGenFile = SKIP_CODE_GEN_PLUGIN.verified().absoluteFile
      val jdepsGenFile = JDEPS_GEN_PLUGIN.verified().absoluteFile

      val kotlinCompilerJar = BazelRunFiles.resolveVerified(
        "external", "com_github_jetbrains_kotlin", "lib", "kotlin-compiler.jar")

      val jvmAbiGenFile = jvmAbiGenPath.verified()
      return KotlinToolchain(
        kotlinCompilerJar.toPath().parent.parent,
        createClassLoader(
          javaHome,
          listOf(
            kotlinCompilerJar,
            COMPILER.verified().absoluteFile,
            // plugins *must* be preloaded. Not doing so causes class conflicts
            // (and a NoClassDef err) in the compiler extension interfaces.
            // This may cause issues in accepting user defined compiler plugins.
            jvmAbiGenFile.absoluteFile,
            skipCodeGenFile,
            jdepsGenFile
          )
        ),
        jvmAbiGen = CompilerPlugin(
          jvmAbiGenFile.absolutePath,
          "org.jetbrains.kotlin.jvm.abi"),
        skipCodeGen = CompilerPlugin(
          skipCodeGenFile.absolutePath,
          "io.bazel.kotlin.plugin.SkipCodeGen"),
        jdepsGen = CompilerPlugin(
          jdepsGenFile.absolutePath,
          "io.bazel.kotlin.plugin.jdeps.JDepsGen"
        )
      )
    }
  }

  data class CompilerPlugin(val jarPath: String, val id: String)

  @Singleton
  class JavacInvoker @Inject constructor(toolchain: KotlinToolchain) {
    private val c = toolchain.classLoader.loadClass("com.sun.tools.javac.Main")
    private val m = c.getMethod("compile", Array<String>::class.java)
    private val mPw = c.getMethod("compile", Array<String>::class.java, PrintWriter::class.java)
    fun compile(args: Array<String>) = m.invoke(c, args) as Int
    fun compile(args: Array<String>, out: PrintWriter) = mPw.invoke(c, args, out) as Int
  }

  @Singleton
  class JDepsInvoker @Inject constructor(toolchain: KotlinToolchain) {
    private val clazz = toolchain.classLoader.loadClass("com.sun.tools.jdeps.Main")
    private val method = clazz.getMethod("run", Array<String>::class.java, PrintWriter::class.java)
    fun run(args: Array<String>, out: PrintWriter): Int = method.invoke(clazz, args, out) as Int
  }

  open class KotlinCliToolInvoker internal constructor(
    toolchain: KotlinToolchain,
    clazz: String
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
    fun compile(args: Array<String>, out: PrintStream): Int {
      val exitCodeInstance = execMethod.invoke(compiler, out, args)
      return getCodeMethod.invoke(exitCodeInstance, *NO_ARGS) as Int
    }
  }

  @Singleton
  class KotlincInvoker @Inject constructor(
    toolchain: KotlinToolchain
  ) : KotlinCliToolInvoker(toolchain, "io.bazel.kotlin.compiler.BazelK2JVMCompiler")

  @Singleton
  class K2JSCompilerInvoker @Inject constructor(
    toolchain: KotlinToolchain
  ) : KotlinCliToolInvoker(toolchain, "org.jetbrains.kotlin.cli.js.K2JSCompiler")
}
