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


class PluginAspectRendering(BazelKotlinTestCase):
    def test_java_plugin(self):
        jar = self.buildJarGetZipFile("ap_kotlin", "jar", silent=False)
        worker_args = self.getWorkerArgsMap("ap_kotlin")
        self.assertIn("--kt-plugins", worker_args)
        self.assertDictContainsSubset({
            "processors": [{
                "processor_class": "com.google.auto.value.processor.AutoValueProcessor",
                "classpath": ["external/autovalue/jar/auto-value-1.5.jar"],
                "generates_api": True
            }]
        }, json.loads(worker_args["--kt-plugins"]))
        self.assertJarContains(
            jar,
            "tests/smoke/kapt/kotlin/AutoValue_TestKtValue.class"
        )


if __name__ == '__main__':
    unittest.main()
