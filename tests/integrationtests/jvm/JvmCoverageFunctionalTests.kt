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
package io.bazel.kotlin.testing.jvm

import io.bazel.kotlin.testing.AssertionTestCase
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import com.google.common.truth.Truth.assertWithMessage
import java.util.stream.Collectors
import kotlin.test.fail

class JvmCoverageFunctionalTests : AssertionTestCase("tests/integrationtests/jvm/coverage") {

    @Test
    fun testCoverage() {
        val coverageOutputDir = Files.createTempDirectory(
                Paths.get(System.getenv("TEST_TMPDIR")),
                "coverage")
        assertExecutableRunfileSucceeds(
                "foo_test",
                description = "Code coverage should by default be collected for :foo but not :foo_test",
                environment = mapOf(
                        "JAVA_COVERAGE_FILE" to coverageOutputDir.resolve("coverage.dat").toString(),
                        "RUNPATH" to Paths.get("").toAbsolutePath().toString() + "/"))
        val coverageReports = Files.list(coverageOutputDir)
                .filter { it.toString().endsWith(".dat") }
                .collect(Collectors.toList())
        assertWithMessage("expected one and only one coverage report").that(coverageReports).hasSize(1)
        val coverageReportLines = Files.readAllLines(coverageReports[0])
        assertWithMessage("unexpected coverage report").that(coverageReportLines).isEqualTo(listOf(
                "SF:simple/Foo.kt",
                "FN:4,simple/Foo::exampleA ()Ljava/lang/String;",
                "FNDA:1,simple/Foo::exampleA ()Ljava/lang/String;",
                "FN:5,simple/Foo::exampleB ()Ljava/lang/String;",
                "FNDA:0,simple/Foo::exampleB ()Ljava/lang/String;",
                "FN:3,simple/Foo::<init> ()V",
                "FNDA:1,simple/Foo::<init> ()V",
                "DA:3,3",
                "DA:4,2",
                "DA:5,0",
                "end_of_record"))
    }
}
