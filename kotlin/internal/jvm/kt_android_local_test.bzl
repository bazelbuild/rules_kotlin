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
load(
    "@rules_android//rules/android_local_test:attrs.bzl",
    _BASE_ATTRS = "ATTRS",
)
load(
    "@rules_android//rules/android_local_test:rule.bzl",
    _make_rule = "make_rule",
)
load(
    "//kotlin/internal:defs.bzl",
    _JAVA_RUNTIME_TOOLCHAIN_TYPE = "JAVA_RUNTIME_TOOLCHAIN_TYPE",
    _JAVA_TOOLCHAIN_TYPE = "JAVA_TOOLCHAIN_TYPE",
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:jvm.bzl",
    _lib_common_attr_exposed = "lib_common_attr_exposed",
    _runnable_common_attr_exposed = "runnable_common_attr_exposed",
)
load(
    "//kotlin/internal/jvm:kt_android_local_test_impl.bzl",
    _kt_android_local_test_impl = "kt_android_local_test_impl",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

_ATTRS = _utils.add_dicts(_BASE_ATTRS, _runnable_common_attr_exposed, {
    "main_class": attr.string(default = "com.google.testing.junit.runner.BazelTestRunner"),
    "jacocorunner": attr.label(
        default = Label("@bazel_tools//tools/jdk:JacocoCoverage"),
    ),
})

kt_android_local_test = _make_rule(
    implementation = _kt_android_local_test_impl,
    attrs = _ATTRS,
    additional_toolchains = [_TOOLCHAIN_TYPE, _JAVA_RUNTIME_TOOLCHAIN_TYPE],
)
