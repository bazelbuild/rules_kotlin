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

import com.google.inject.*
import com.google.inject.util.Modules
import io.bazel.kotlin.builder.utils.executeAndAwait
import io.bazel.kotlin.builder.utils.resolveVerified
import io.bazel.kotlin.builder.utils.verifiedRelativeFiles
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths

class KotlinToolchain constructor(
    val kotlinLibraryDirectory: Path,
    val kotlinStandardLibraries: Array<String> = arrayOf(
        "kotlin-stdlib.jar",
        "kotlin-stdlib-jdk7.jar",
        "kotlin-stdlib-jdk8.jar"
    )
) : AbstractModule() {
    companion object {
        internal val NO_ARGS = arrayOf<Any>()

        /**
         * @param outputProvider A provider for the output stream to write to. A provider is used here as the System.err
         * gets rebound when the worker is executing.
         */
        @JvmStatic
        fun createInjector(outputProvider: Provider<PrintStream>, overrides: Module? = null): Injector =
            Guice.createInjector(
                object : AbstractModule() {
                    override fun configure() {
                        val builderRunfiles=Paths.get(System.getenv("JAVA_RUNFILES"))
                        bind(PrintStream::class.java).toProvider(outputProvider)
                        install(
                            KotlinToolchain.TCModule(
                                javaHome = Paths.get("external", "local_jdk"),
                                kotlinHome = Paths.get("external", "com_github_jetbrains_kotlin"),
                                bazelKotlinCompilersJar = builderRunfiles.resolveVerified(
                                    "io_bazel_rules_kotlin", "kotlin", "builder","compiler_lib.jar")
                            )
                        )
                    }
                }.let { module -> overrides?.let { Modules.override(module).with(it) } ?: module }
            )
    }

    data class CompilerPlugin(val jarPath: String, val id: String) {
        @BindingAnnotation
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

    interface JarToolInvoker {
        fun invoke(args: List<String>, directory: File? = null)
    }

    private class TCModule constructor(
        javaHome: Path,
        kotlinHome: Path,
        bazelKotlinCompilersJar: File,
        kotlinLibraryDirectory: Path = kotlinHome.resolveVerified("lib").toPath(),
        kapt3Jar: File = kotlinLibraryDirectory.resolveVerified("kotlin-annotation-processing.jar"),
        classloader: ClassLoader = ClassPreloadingUtils.preloadClasses(
            mutableListOf<File>().let {
                it.add(bazelKotlinCompilersJar)
                it.addAll(kotlinLibraryDirectory.verifiedRelativeFiles(Paths.get("kotlin-compiler.jar")))
                it.addAll(javaHome.verifiedRelativeFiles(Paths.get("lib", "tools.jar")))
                it.toList()
            },
            Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
            Thread.currentThread().contextClassLoader,
            null
        )
    ) : AbstractModule() {
        private val toolchain = KotlinToolchain(kotlinHome)

        private val kapt3 = CompilerPlugin(kapt3Jar.toString(), "org.jetbrains.kotlin.kapt3")

        private val jarToolInvoker = object : JarToolInvoker {
            val jarToolPath = javaHome.resolveVerified("bin", "jar").absolutePath.toString()
            override fun invoke(args: List<String>, directory: File?) {
                val command = mutableListOf(jarToolPath).also { it.addAll(args) }
                executeAndAwait(10, directory, command).takeIf { it != 0 }?.also {
                    throw CompilationStatusException("error running jar command ${command.joinToString(" ")}", it)
                }
            }
        }

        private val javacInvoker = object : JavacInvoker {
            val c = classloader.loadClass("com.sun.tools.javac.Main")
            val m = c.getMethod("compile", Array<String>::class.java)
            val mPw = c.getMethod("compile", Array<String>::class.java, PrintWriter::class.java)
            override fun compile(args: Array<String>) = m.invoke(c, args) as Int
            override fun compile(args: Array<String>, out: PrintWriter) = mPw.invoke(c, args, out) as Int
        }

        private val jdepsInvoker = object : JDepsInvoker {
            val clazz = classloader.loadClass("com.sun.tools.jdeps.Main")
            val method = clazz.getMethod("run", Array<String>::class.java, PrintWriter::class.java)
            override fun run(args: Array<String>, out: PrintWriter): Int = method.invoke(clazz, args, out) as Int
        }

        private val kotlincInvoker = object : KotlincInvoker {
            val compilerClass = classloader.loadClass("io.bazel.kotlin.compiler.BazelK2JVMCompiler")
            val exitCodeClass = classloader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode")

            val compiler = compilerClass.getConstructor().newInstance()
            val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
            val getCodeMethod = exitCodeClass.getMethod("getCode")


            override fun compile(args: Array<String>, out: PrintStream): Int {
                val exitCodeInstance = execMethod.invoke(compiler, out, args)
                return getCodeMethod.invoke(exitCodeInstance, *NO_ARGS) as Int
            }
        }

        override fun configure() {
            bind(KotlinToolchain::class.java).toInstance(toolchain)
            bind(JarToolInvoker::class.java).toInstance(jarToolInvoker)
            bind(JavacInvoker::class.java).toInstance(javacInvoker)
            bind(JDepsInvoker::class.java).toInstance(jdepsInvoker)
            bind(KotlincInvoker::class.java).toInstance(kotlincInvoker)
        }

        @Provides
        @CompilerPlugin.Kapt3
        fun provideKapt3(): CompilerPlugin = kapt3

        @Provides
        fun provideBazelWorker(
            kotlinBuilder: KotlinBuilder,
            output: PrintStream
        ): BazelWorker = BazelWorker(kotlinBuilder, output, "KotlinCompile")
    }
}


