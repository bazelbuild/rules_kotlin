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
package(default_visibility=["//visibility:private"])
# The tests are written in Kotlin, so the tests inately test a lot of the rule functionality.
load("//tests/rules:defs.bzl", "kt_it_assertion_test")

kt_it_assertion_test(
    name = "basic_tests",
    cases="//tests/integrationtests/jvm/basic:cases",
    test_class="io.bazel.kotlin.testing.jvm.JvmBasicFunctionalTests",
)

kt_it_assertion_test(
    name = "annoation_processing_tests",
    cases = "//tests/integrationtests/jvm/kapt:cases",
    test_class="io.bazel.kotlin.testing.jvm.JvmAnnotationProcessingFunctionalTests",
    data = [  "//examples/dagger:coffee_app"]
)
