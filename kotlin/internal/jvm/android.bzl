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
    "//kotlin/internal/jvm:jvm.bzl",
    _kt_jvm_library = "kt_jvm_library",
)

def _kt_android_artifact(name, srcs = [], deps = [], plugins = [], friend = None, **kwargs):
    """Delegates Android related build attributes to the native rules but uses the Kotlin builder to compile Java and
    Kotlin srcs. Returns a sequence of labels that a wrapping macro should export.
    """
    base_name = name + "_base"
    kt_name = name + "_kt"

    base_deps = deps + ["@io_bazel_rules_kotlin//kotlin/internal/jvm:android_sdk"]

    native.android_library(
        name = base_name,
        visibility = ["//visibility:private"],
        exports = base_deps,
        **kwargs
    )
    _kt_jvm_library(
        name = kt_name,
        srcs = srcs,
        deps = base_deps + [base_name],
        plugins = plugins,
        testonly = kwargs.get("testonly", default = 0),
        friend = friend,
        # must be public to be referenced as friends.
        # TODO: rework this into a proper android provider giving rule, so we can avoid all this.
        visibility = ["//visibility:public"],
    )
    return [base_name, kt_name]

def kt_android_library(name, exports = [], visibility = None, **kwargs):
    """Creates an Android sandwich library. `srcs`, `deps`, `plugins` are routed to `kt_jvm_library` the other android
    related attributes are handled by the native `android_library` rule.
    """
    native.android_library(
        name = name,
        exports = exports + _kt_android_artifact(name, **kwargs),
        visibility = visibility,
        testonly = kwargs.get("testonly", default = 0),
    )
