# Copyright 2024 The Bazel Authors. All rights reserved.
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

"""Integration tests for the compat implementation of kt_jvm_test and kt_jvm_binary."""

load("//kotlin:jvm_compat.bzl", "kt_jvm_binary", "kt_jvm_test")

package(default_visibility = ["//visibility:private"])

# Test the compat implementation of kt_jvm_test
kt_jvm_test(
    name = "JvmCompatTest",
    srcs = ["JvmCompatTest.kt"],
    test_class = "jvm_compat.JvmCompatTest",
    deps = [
        "@kotlin_rules_maven//:junit_junit",
    ],
)

# Test the compat implementation of kt_jvm_binary
kt_jvm_binary(
    name = "jvm_compat_binary",
    srcs = ["Main.kt"],
    main_class = "jvm_compat.MainKt",
)

# Filegroup for build verification
filegroup(
    name = "jvm_compat",
    testonly = True,
    srcs = [
        ":JvmCompatTest",
        ":jvm_compat_binary",
    ],
    visibility = ["//visibility:public"],
)
