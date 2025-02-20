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
    "@rules_android//android:rules.bzl",
    _android_library = "android_library",
    _android_local_test = "android_local_test",
)
load(
    "//kotlin/internal/jvm:jvm.bzl",
    _kt_jvm_library = "kt_jvm_library",
)

_ANDROID_SDK_JAR = "%s" % Label("//third_party:android_sdk")

def _kt_android_artifact(
        name,
        srcs = [],
        deps = [],
        resources = [],
        plugins = [],
        associates = [],
        module_name = "",
        kotlinc_opts = None,
        javac_opts = None,
        enable_data_binding = False,
        tags = [],
        exec_properties = None,
        **kwargs):
    """Delegates Android related build attributes to the native rules but uses the Kotlin builder to compile Java and
    Kotlin srcs. Returns a sequence of labels that a wrapping macro should export.
    """
    base_name = name + "_base"
    kt_name = name + "_kt"

    # TODO(bazelbuild/rules_kotlin/issues/273): This should be retrieved from a provider.
    base_deps = [_ANDROID_SDK_JAR] + deps

    _android_library(
        name = base_name,
        visibility = ["//visibility:private"],
        exports = base_deps,
        deps = deps if enable_data_binding else [],
        enable_data_binding = enable_data_binding,
        tags = tags,
        exec_properties = exec_properties,
        **kwargs
    )
    _kt_jvm_library(
        name = kt_name,
        srcs = srcs,
        deps = [base_name] + base_deps,
        resources = resources,
        plugins = plugins,
        associates = associates,
        module_name = module_name,
        testonly = kwargs.get("testonly", default = False),
        visibility = ["//visibility:public"],
        kotlinc_opts = kotlinc_opts,
        javac_opts = javac_opts,
        tags = tags,
        exec_properties = exec_properties,
    )
    return [base_name, kt_name]

def kt_android_library(name, exports = [], visibility = None, exec_properties = None, **kwargs):
    """Creates an Android sandwich library.

    `srcs`, `deps`, `plugins` are routed to `kt_jvm_library` the other android
    related attributes are handled by the native `android_library` rule.
    """

    _android_library(
        name = name,
        exports = exports + _kt_android_artifact(name, exec_properties = exec_properties, **kwargs),
        visibility = visibility,
        tags = kwargs.get("tags", default = None),
        testonly = kwargs.get("testonly", default = 0),
        exec_properties = exec_properties,
    )

def kt_android_local_test(
        name,
        jvm_flags = None,
        manifest = None,
        manifest_values = None,
        test_class = None,
        size = None,
        data = None,
        timeout = None,
        flaky = False,
        shard_count = None,
        visibility = None,
        testonly = True,
        exec_properties = None,
        nocompress_extensions = None,
        **kwargs):
    """Creates a testable Android sandwich library.

    `srcs`, `deps`, `plugins`, `associates` are routed to `kt_jvm_library` the other android
    related attributes are handled by the native `android_library` rule while the test attributes
    are picked out and handled by the `android_local_test` rule.
    """

    _android_local_test(
        name = name,
        deps = kwargs.get("deps", []) + _kt_android_artifact(name = name, testonly = testonly, exec_properties = exec_properties, **kwargs),
        jvm_flags = jvm_flags,
        test_class = test_class,
        visibility = visibility,
        size = size,
        data = data,
        timeout = timeout,
        flaky = flaky,
        shard_count = shard_count,
        custom_package = kwargs.get("custom_package", default = None),
        manifest = manifest,
        manifest_values = manifest_values,
        tags = kwargs.get("tags", default = None),
        testonly = testonly,
        exec_properties = exec_properties,
        nocompress_extensions = nocompress_extensions,
    )
