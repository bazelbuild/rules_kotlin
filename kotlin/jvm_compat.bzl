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

"""Compatibility implementation of kt_jvm_test and kt_jvm_binary.

This module provides alternative implementations of kt_jvm_test and kt_jvm_binary
that compose kt_jvm_library with native java_test/java_binary rules. This approach
provides better Windows compatibility by leveraging Bazel's native Java rules.

Usage:
    # Instead of:
    # load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test", "kt_jvm_binary")

    # Use:
    load("@rules_kotlin//kotlin:jvm_compat.bzl", "kt_jvm_test", "kt_jvm_binary")

    kt_jvm_test(
        name = "MyTest",
        srcs = ["MyTest.kt"],
        deps = ["//my:lib"],
    )

    kt_jvm_binary(
        name = "my_app",
        srcs = ["Main.kt"],
        main_class = "com.example.MainKt",
    )

The API is identical to the standard kt_jvm_test and kt_jvm_binary rules,
but internally creates:
- A kt_jvm_library target for Kotlin compilation (named "<name>_lib")
- A java_test or java_binary target for execution (named "<name>")
"""

load(
    "//kotlin/internal:opts.bzl",
    _kt_javac_options = "kt_javac_options",
)
load(
    "//kotlin/internal/jvm:jvm.bzl",
    _kt_jvm_import = "kt_jvm_import",
    _kt_jvm_library = "kt_jvm_library",
)
load(
    "//kotlin/internal/jvm:jvm_compat_macros.bzl",
    _kt_jvm_binary_compat = "kt_jvm_binary_compat",
    _kt_jvm_test_compat = "kt_jvm_test_compat",
)

# Re-export compat implementations with standard names for drop-in replacement
kt_jvm_binary = _kt_jvm_binary_compat
kt_jvm_test = _kt_jvm_test_compat

# Re-export unchanged rules for convenience
kt_jvm_library = _kt_jvm_library
kt_jvm_import = _kt_jvm_import
kt_javac_options = _kt_javac_options
