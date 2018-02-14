# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import json
import unittest

from common import BazelKotlinTestCase

_auto_value_processor_entry = {
    "label": "//tests/smoke:autovalue",
    "processor_class": "com.google.auto.value.processor.AutoValueProcessor",
    "classpath": ["external/autovalue/jar/auto-value-1.5.jar"],
    "generates_api": True
}

_auto_service_processor_entry = {
    'label': '//tests/smoke:autoservice',
    'classpath': [
        'external/autoservice/jar/auto-service-1.0-rc4.jar',
        'external/guava/jar/guava-24.0-jre.jar',
        'external/auto_common/jar/auto-common-0.10.jar'
    ],
    'processor_class': 'com.google.auto.service.processor.AutoServiceProcessor',
    'generates_api': False,
}


class PluginAspectRendering(BazelKotlinTestCase):
    def test_annotation_processing(self):
        """Annotation processing should occur for Kotlin files when a java_plugin is provided."""
        jar = self.buildJarGetZipFile("ap_kotlin", "jar", silent=False)
        worker_args = self.getWorkerArgsMap()
        self.assertIn("--kt-plugins", worker_args)
        self.assertDictContainsSubset({
            "processors": [_auto_value_processor_entry]
        }, json.loads(worker_args["--kt-plugins"]))
        self.assertJarContains(
            jar,
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class"
        )

    def test_annotation_processing_no_plugin_provided(self):
        """If no plugins are provided in the rule annotation processing should not occur."""
        jar = self.buildJarGetZipFile("ap_kotlin_mixed_no_plugin", "jar")
        worker_args = self.getWorkerArgsMap()
        self.assertNotIn("--kt-plguins", worker_args)
        self.assertJarDoesNotContain(
            jar,
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
            "tests/smoke/kapt/java/TestAutoValue$Builder.class"
        )

    def test_annotation_processing_mixed(self):
        """If plugins are provided for a mixed mode library annotation processing should occur for both Kotlin and Java files."""
        jar = self.buildJarGetZipFile("ap_kotlin_mixed", "jar", silent=False)
        self.assertJarContains(
            jar,
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
            "tests/smoke/kapt/java/TestAutoValue$Builder.class"
        )

    def test_annotation_processing_with_resources(self):
        """If a plugin generating resources is provided it's resources should be embedded in the jar."""
        jar = self.buildJarGetZipFile("ap_kotlin_resources", "jar")
        self.assertJarContains(
            jar,
            "META-INF/services/tests.smoke.kapt.kotlin.TestKtService"
        )

    def test_annotation_processing_with_resources_mixed(self):
        """If a plugin generating resources is provided for a mixed mode library resources shsould be embedded in the jar for both Kotlin and Java Files."""
        jar = self.buildJarGetZipFile("ap_kotlin_resources_mixed", "jar")
        self.assertJarContains(
            jar,
            "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
            "META-INF/services/tests.smoke.kapt.java.TestJavaService"
        )

    def test_annotation_processing_with_mutliple_plugins_mixed(self):
        """Annotation processing should work for multiple plugins."""
        jar = self.buildJarGetZipFile("ap_kotlin_mixed_multiple_plugins", "jar")
        self.assertJarContains(
            jar,
            "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
            "META-INF/services/tests.smoke.kapt.java.TestJavaService",
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
            "tests/smoke/kapt/java/TestAutoValue$Builder.class"
        )

    def test_annotation_processing_with_mutliple_plugins_mixed_one_without_processor_class(self):
        """Annotation processing should not trigger for plugins which do not provide a java processor class."""
        jar = self.buildJarGetZipFile("ap_kotlin_mixed_multiple_plugins_one_without_processor_class", "jar")
        self.assertJarContains(
            jar,
            "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
            "META-INF/services/tests.smoke.kapt.java.TestJavaService",
        )
        self.assertJarDoesNotContain(
            jar,
            "tests/smoke/kapt/java/AutoValue_TestAPNoGenReferences.class",
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValueNoReferences.class"
        )

    def test_annotation_processing_with_multiple_plugins_inherit_exported_plugin(self):
        """Annotation processing should work when a plugin is inherited from another library via expoted_plugins, and plugins should not be counted twice."""
        jar = self.buildJarGetZipFile("ap_kotlin_mixed_inherit_plugin_via_exported_deps", "jar")
        worker_args = self.getWorkerArgsMap()
        self.assertIn("--kt-plugins", worker_args)
        self.assertDictContainsSubset({
            "processors": [_auto_value_processor_entry, _auto_service_processor_entry]
        }, json.loads(worker_args["--kt-plugins"]))
        self.assertJarContains(
            jar,
            "META-INF/services/tests.smoke.kapt.kotlin.TestKtService",
            "META-INF/services/tests.smoke.kapt.java.TestJavaService",
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class",
            "tests/smoke/kapt/java/TestAutoValue$Builder.class"
        )

    def test_annotation_processing_compile_and_run_kapt_example(self):
        """A Self contained example annotation processing package should compile and launch without errors."""
        self.buildLaunchExpectingSuccess("//examples/dagger:coffee_app")


if __name__ == '__main__':
    unittest.main()
