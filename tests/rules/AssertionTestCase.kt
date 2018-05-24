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
package io.bazel.kotlin.testing

import com.google.common.base.Throwables
import com.google.inject.Injector
import com.google.inject.Provider
import io.bazel.kotlin.builder.BuildCommandBuilder
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.ArgMaps
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.test.fail

class TestCaseFailedException(description: String? = null, ex: Throwable) :
    AssertionError(""""$description" failed, error: ${Throwables.getRootCause(ex).message}""")

abstract class AssertionTestCase(root: String) {
    companion object {
        @JvmStatic
        private val injector: Injector by lazy { KotlinToolchain.createInjector(Provider { System.err }) }
    }

    protected val commandBuilder: BuildCommandBuilder by lazy { injector.getInstance(BuildCommandBuilder::class.java) }

    private val testRunfileRoot: Path = Paths.get(root).also {
        it.toFile().also {
            assert(it.exists()) { "runfile directory $root does not exist" }
            assert(it.isDirectory) { "runfile directory $root is not a directory" }
        }
    }

    private inline fun runTestCase(description: String? = null, op: () -> Unit) =
        try {
            op()
        } catch (t: Throwable) {
            when (t) {
                is AssertionError -> throw TestCaseFailedException(description, t)
                is Exception -> throw TestCaseFailedException(description, t)
                else -> throw t
            }
        }

    private fun testCaseFile(fileName: String) = testRunfileRoot.resolve(fileName).toFile().also {
        check(it.exists()) { "file $fileName did not exist in test case root $testRunfileRoot" }
    }

    protected fun jarTestCase(name: String, description: String? = null, test: JarFile.() -> Unit) {
        runTestCase(description, { testCaseFile(name).let { JarFile(it) }.also { it.test() } })
    }

    protected fun argMapTestCase(name: String, description: String? = null, test: ArgMap.() -> Unit) {
        val file = testCaseFile(name)
        ArgMaps.from(file).test()
    }


    protected fun JarFile.assertContainsEntries(vararg entries: String) {
        entries.forEach {
            if (this.getJarEntry(it) == null) {
                fail("jar ${this.name} did not contain entry $it")
            }
        }
    }

    protected fun JarFile.assertDoesNotContainEntries(vararg entries: String) {
        entries.forEach {
            if (this.getJarEntry(it) != null) {
                fail("jar ${this.name} contained entry $it")
            }
        }
    }

    private fun String.resolveDirectory(): File =
        if (startsWith("/"))
            trimStart('/').split("/").let { File(it.take(it.size - 1).joinToString(File.separator)) }
        else
            testRunfileRoot.toFile()

    protected fun assertExecutableRunfileSucceeds(executable: String, description: String? = null) {
        ProcessBuilder().command("bash", "-c", Paths.get(executable).fileName.toString())
            .also { it.directory(executable.resolveDirectory()) }
            .start().let {
                it.waitFor(5, TimeUnit.SECONDS)
                assert(it.exitValue() == 0) {
                    throw TestCaseFailedException(description, RuntimeException("non-zero return code: ${it.exitValue()}"))
                }
            }
    }
}