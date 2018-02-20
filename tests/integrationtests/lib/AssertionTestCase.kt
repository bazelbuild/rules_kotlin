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
package io.bazel.ruleskotlin.integrationtests.lib

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.test.fail

class TestCaseFailedException(description: String? = null, ex: Throwable):
        AssertionError(""""$description" failed, error: ${ex.message}""", ex)

abstract class AssertionTestCase(root: String) {
    private val testRunfileRoot: Path = Paths.get(root).also {
        it.toFile().also {
            assert(it.exists()) { "runfile directory $root does not exist" }
            assert(it.isDirectory) { "runfile directory $root is not a directory" }
        }
    }

    private inline fun runTestCase(description: String? = null, op: () -> Unit) =
            try { op() }
            catch(t: Throwable) {
                when(t) {
                    is AssertionError -> throw TestCaseFailedException(description, t)
                    is Exception -> throw TestCaseFailedException(description, t)
                    else -> throw t
                }
            }

    private fun testCaseJar(jarName: String) = testRunfileRoot.resolve(jarName).toFile().let {
        check(it.exists()) { "jar $jarName did not exist in test case root $testRunfileRoot" }
        JarFile(it)
    }

    private fun jarTestCase(name: String, op: JarFile.() -> Unit) { testCaseJar(name).also { op(it) } }
    protected fun jarTestCase(name: String, description: String? = null, op: JarFile.() -> Unit) { runTestCase(description, { jarTestCase(name, op) }) }


    protected fun JarFile.assertContainsEntries(vararg entries: String) {
        entries.forEach { if(this.getJarEntry(it) == null) { fail("jar ${this.name} did not contain entry $it") } }
    }

    protected fun JarFile.assertDoesNotContainEntries(vararg entries: String) {
        entries.forEach { if(this.getJarEntry(it) != null) { fail("jar ${this.name} contained entry $it") } }
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