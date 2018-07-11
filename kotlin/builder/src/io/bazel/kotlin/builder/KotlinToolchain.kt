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

import com.google.common.collect.ImmutableSet
import com.google.inject.*
import com.google.inject.util.Modules
import io.bazel.kotlin.builder.utils.resolveVerified
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Qualifier

class KotlinToolchain private constructor(
    internal val javaHome: Path,
    val kotlinHome: Path,
    internal val classLoader: ClassLoader,
    val kotlinStandardLibraries: ImmutableSet<String> = ImmutableSet.of(
        "kotlin-stdlib.jar",
        "kotlin-stdlib-jdk7.jar",
        "kotlin-stdlib-jdk8.jar"
    )
) {
    companion object {
        internal val NO_ARGS = arrayOf<Any>()

        private val isJdk9OrNewer = !System.getProperty("java.version").startsWith("1.")
        private val javaRunfiles get() = Paths.get(System.getenv("JAVA_RUNFILES"))

        private fun createClassLoader(javaHome: Path, kotlinHome: Path): ClassLoader {
            val preloadJars = mutableListOf<File>().also {
                it += kotlinHome.resolveVerified("lib", "kotlin-compiler.jar")
                it +=  javaRunfiles.resolveVerified("io_bazel_rules_kotlin", "kotlin", "builder", "compiler_lib.jar")
                if(!isJdk9OrNewer) {
                    it += javaHome.resolveVerified("lib", "tools.jar")
                }
            }
            return ClassPreloadingUtils.preloadClasses(
                preloadJars,
                Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                ClassLoader.getSystemClassLoader(),
                null
            )
        }

        private fun createToolchain(): KotlinToolchain {
            val kotlinHome = Paths.get("external", "com_github_jetbrains_kotlin")
            val javaHome = Paths.get(System.getProperty("java.home")).let {
                it.takeIf { !it.endsWith(Paths.get("jre")) } ?: it.parent
            }
            return KotlinToolchain(javaHome, kotlinHome, createClassLoader(javaHome, kotlinHome))
        }

        /**
         * @param outputProvider A provider for the output stream to write to. A provider is used here as the System.err
         * gets rebound when the worker is executing.
         */
        @JvmStatic
        fun createInjector(outputProvider: Provider<PrintStream>, overrides: Module? = null): Injector {
            val toolchain = createToolchain()
            val module = object : AbstractModule() {
                override fun configure() {
                    bind(KotlinToolchain::class.java).toInstance(toolchain)
                    bind(PrintStream::class.java).toProvider(outputProvider)
                    install(KotlinToolchainModule)
                }
            }
            return Guice.createInjector(
                overrides?.let { Modules.override(module).with(it) } ?: module
            )
        }
    }

    data class CompilerPlugin(val jarPath: String, val id: String) {
        @Qualifier
        annotation class Kapt3
    }

    interface JavacInvoker {
        fun compile(args: Array<String>): Int
        fun compile(args: Array<String>, out: PrintWriter): Int
    }


    interface JDepsInvoker {
        fun run(args: Array<String>, out: PrintWriter): Int
    }


    interface KotlincInvoker {
        fun compile(args: Array<String>, out: PrintStream): Int
    }
}


