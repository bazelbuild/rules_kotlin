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

    internal val KOTLINC by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...kotlin-compiler",
        ).toPath()
    }

    private val KOTLIN_REFLECT by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@rules_kotlin..kotlin.compiler.kotlin-reflect",
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

    private val KOTLIN_EMBEDDED by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@kotlin_daemon_embeddable...kotlin-daemon-embeddable",
        ).toPath()
    }

    private val KOTLIN_DAEMON by lazy {
      BazelRunFiles
        .resolveVerifiedFromProperty(
          "@com_github_jetbrains_kotlin...kotlin-daemon-client",
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

    /**
     * Creates an isolated classloader for Build Tools API operations.
     *
     * This avoids class conflicts between the preloaded compiler (which has
     * non-relocated IntelliJ classes) and the Build Tools implementation
     * (which expects relocated classes).
     *
     * Order matters! kotlin-compiler-embeddable must come before kotlinc so that:
     * - Relocated IntelliJ classes (org.jetbrains.kotlin.com.intellij.*) come from embeddable
     * - Regular kotlin.* classes can come from either (embeddable has them too, or kotlinc as fallback)
     */
    internal fun createIsolatedBuildToolsClassLoader(
      buildToolsApiJar: File,
      buildToolsImplJar: File,
      compilerJar: File,
      kotlinCompilerEmbeddableJar: File,
      kotlincJar: File,
    ): ClassLoader {
      // Step 1: Create bootstrap classloader with just build-tools-api
      // Uses system classloader as parent (no IntelliJ class conflicts)
      val bootstrapClassLoader = URLClassLoader(
        arrayOf(buildToolsApiJar.toURI().toURL()),
        ClassLoader.getSystemClassLoader(),
      )

      // Step 2: Get SharedApiClassesClassLoader from bootstrap
      // This returns a classloader that:
      // - Uses JDK classes as base
      // - Delegates org.jetbrains.kotlin.buildtools.api.* to bootstrap classloader
      val sharedApiClassLoaderFactory = bootstrapClassLoader.loadClass(
        "org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader",
      )
      val sharedApiClassLoader = sharedApiClassLoaderFactory
        .getMethod("newInstance")
        .invoke(null) as ClassLoader

      // Step 3: Create implementation classloader with all jars
      // Uses SharedApiClassesClassLoader as parent for proper isolation
      // Order: embeddable first (for relocated IntelliJ), then kotlinc (for kotlin.*)
      return URLClassLoader(
        arrayOf(
          buildToolsApiJar.toURI().toURL(),
          buildToolsImplJar.toURI().toURL(),
          kotlinCompilerEmbeddableJar.toURI().toURL(),
          kotlincJar.toURI().toURL(),
          compilerJar.toURI().toURL(),
        ),
        sharedApiClassLoader,
      )
    }

    @JvmStatic
    fun createToolchain(): KotlinToolchain =
      createToolchain(
        KOTLINC.verified().absoluteFile,
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
        KOTLIN_EMBEDDED.toFile(),
        KOTLIN_DAEMON.toFile(),
      )

    @JvmStatic
    fun createToolchain(
      kotlinc: File,
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
      kotlinEmbedded: File,
      kotlinDaemon: File,
    ): KotlinToolchain =
      KotlinToolchain(
        listOf(
          kotlinc,
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
          kotlinEmbedded,
          kotlinDaemon,
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

      toolchain.classLoader.loadClass(
        "org.jetbrains.kotlin.buildtools.internal.CompilationServiceProxy",
      )
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

  class ClasspathSnapshotInvoker internal constructor(
    buildToolsApiJar: File,
    buildToolsImplJar: File,
    compilerJar: File,
    kotlinCompilerEmbeddableJar: File,
    kotlincJar: File,
  ) {
    private val generateMethod: Method

    init {
      // Create fully isolated classloader - no preloaded compiler classes!
      // This avoids ClassCastException from mixing relocated/non-relocated IntelliJ classes.
      val isolatedClassLoader = createIsolatedBuildToolsClassLoader(
        buildToolsApiJar,
        buildToolsImplJar,
        compilerJar,
        kotlinCompilerEmbeddableJar,
        kotlincJar,
      )

      val clazz = isolatedClassLoader.loadClass(
        "io.bazel.kotlin.compiler.ClasspathSnapshotGenerator",
      )
      generateMethod = clazz.getMethod(
        "generate",
        String::class.java,
        String::class.java,
        String::class.java,
      )
    }

    fun generate(inputJar: String, outputSnapshot: String, granularity: String) {
      generateMethod.invoke(null, inputJar, outputSnapshot, granularity)
    }
  }

  @Singleton
  class KotlincInvokerBuilder
    @Inject
    constructor(
      private val toolchain: KotlinToolchain,
    ) {
      fun build(): KotlincInvoker =
        KotlincInvoker(
          toolchain = toolchain,
          clazz = "io.bazel.kotlin.compiler.BuildToolsAPICompiler",
        )

      fun buildSnapshotInvoker(): ClasspathSnapshotInvoker =
        ClasspathSnapshotInvoker(
          BUILD_TOOLS_API.verified().absoluteFile,
          BUILD_TOOLS_IMPL.verified().absoluteFile,
          COMPILER.verified().absoluteFile,
          KOTLIN_COMPILER_EMBEDDABLE.verified().absoluteFile,
          KOTLINC.verified().absoluteFile,
        )

      fun getClassLoader(): ClassLoader = toolchain.classLoader
    }
}
