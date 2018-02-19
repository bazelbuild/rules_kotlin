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
package io.bazel.ruleskotlin.integrationtests.jvm

import io.bazel.ruleskotlin.integrationtests.lib.AssertionTestCase
import org.junit.Test

class JvmAnnotationProcessingIntegrationTests : AssertionTestCase("tests/integrationtests/jvm/kapt") {
    @Test
    fun kotlinOnly() = withTestCaseJar("ap_kotlin.jar") {
        assertContainsEntries("tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class")
    }

    @Test
    fun noPluginProvided() = withTestCaseJar("ap_kotlin_mixed_no_plugin.jar") {
        assertDoesNotContainEntries(
                "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
                "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
        )
    }

    @Test
    fun mixedMode() = withTestCaseJar("ap_kotlin_mixed.jar") {
        assertContainsEntries(
                "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
                "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
        )
    }

    @Test
    fun withResources() = withTestCaseJar("ap_kotlin_resources.jar") {
        assertContainsEntries("META-INF/services/tests.smoke.kapt.kotlin.TestKtService")
    }

    @Test
    fun mixedModeWithResources() = withTestCaseJar("ap_kotlin_resources_mixed.jar") {
        assertContainsEntries(
                "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
                "META-INF/services/tests.smoke.kapt.java.TestJavaService"
        )
    }

    @Test
    fun withMultiplePlugins() = withTestCaseJar("ap_kotlin_mixed_multiple_plugins.jar") {
        assertContainsEntries(
                "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
                "META-INF/services/tests.smoke.kapt.java.TestJavaService",
                "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
                "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
        )
    }

    @Test
    fun withMultiplePluginsOneWithoutProcessorClassAttribute() = withTestCaseJar("ap_kotlin_mixed_multiple_plugins_one_without_processor_class.jar") {
        assertContainsEntries("META-INF/services/tests.smoke.kapt.kotlin.TestKtService", "META-INF/services/tests.smoke.kapt.java.TestJavaService")
        assertDoesNotContainEntries(
                "tests/smoke/kapt/java/AutoValue_TestAPNoGenReferences.class",
                "tests/smoke/kapt/kotlin/AutoValue_TestKtValueNoReferences.class"
        )
    }

    @Test
    fun withTransitivelyInheritedPlugin() = withTestCaseJar("ap_kotlin_mixed_inherit_plugin_via_exported_deps.jar") {
        assertContainsEntries(
                "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
                "META-INF/services/tests.smoke.kapt.java.TestJavaService",
                "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
                "tests/smoke/kapt/java/TestAutoValue\$Builder.class"
        )
    }

    @Test
    fun daggerExampleIsRunnable() {
        assertExecutableRunfileSucceeds("//examples/dagger/coffee_app")
    }
}