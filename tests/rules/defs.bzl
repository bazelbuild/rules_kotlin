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
load("//kotlin:kotlin.bzl", "kt_jvm_test")

TEST_COMMON_DEPS = [
    "//tests/rules:assertion_test_case",
    "//third_party/jvm/com/google/truth",
    "//third_party/jvm/junit:junit",
]

def kt_it_assertion_test(name, test_class, cases = None, data = [], deps = []):
    parts = test_class.split(".")
    if cases:
        data = data + [cases]
    kt_jvm_test(
        name = name,
        srcs = ["%s.kt" % parts[len(parts) - 1]],
        deps = TEST_COMMON_DEPS + deps,
        test_class = test_class,
        data = data,
        visibility = ["//visibility:private"],
    )
