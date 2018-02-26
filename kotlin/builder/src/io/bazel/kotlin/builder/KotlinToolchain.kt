/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder

import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.verifiedRelativeFiles
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("PropertyName")
class KotlinToolchain {
    companion object {
        internal val JAVA_HOME = Paths.get("external", "local_jdk")
        internal val KOTLIN_HOME = Paths.get("external", "com_github_jetbrains_kotlin")

        internal val NO_ARGS = arrayOf<Any>()
    }

    val JAVAC_PATH = JAVA_HOME.resolveVerified("bin", "javac").toString()
    val JAR_TOOL_PATH = JAVA_HOME.resolveVerified("bin", "jar").toString()
    val JDEPS_PATH = JAVA_HOME.resolveVerified("bin", "jdeps").toString()
    val KOTLIN_LIB_DIR: Path = KOTLIN_HOME.resolveVerified("lib").toPath()

    val KAPT_PLUGIN = CompilerPlugin(KOTLIN_LIB_DIR.resolveVerified("kotlin-annotation-processing.jar").toString(), "org.jetbrains.kotlin.kapt3")

    private val kotlinPreloadJars = mutableListOf<File>().let {
        it.addAll(KOTLIN_LIB_DIR.verifiedRelativeFiles(Paths.get("kotlin-compiler.jar")))

        // tools.jar is need for annotation processing
        it.addAll(JAVA_HOME.verifiedRelativeFiles(Paths.get("lib", "tools.jar")))
        it.toList()
    }

    val KOTLIN_STD_LIBS = arrayOf(
            "kotlin-stdlib.jar",
            "kotlin-stdlib-jdk7.jar",
            "kotlin-stdlib-jdk8.jar"
    )

    private val classLoader: ClassLoader by lazy {
        ClassPreloadingUtils.preloadClasses(
                kotlinPreloadJars,
                Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                Thread.currentThread().contextClassLoader,
                null
        )
    }

    interface KotlinCompiler {
        fun compile(args: Array<String>, out: PrintStream): Int
    }

    data class CompilerPlugin(val jarPath: String, val id: String)

    /**
     * Load the Kotlin compiler into a Preloading classLoader.
     */
    val kotlinCompiler: KotlinCompiler by lazy {
        val compilerClass = classLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        val exitCodeClass = classLoader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode")

        val compiler = compilerClass.newInstance()
        val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        val getCodeMethod = exitCodeClass.getMethod("getCode")

        object : KotlinCompiler {
            override fun compile(args: Array<String>, out: PrintStream): Int {
                val exitCodeInstance: Any
                try {
                    exitCodeInstance = execMethod.invoke(compiler, out, args)
                    return getCodeMethod.invoke(exitCodeInstance, *NO_ARGS) as Int
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}


