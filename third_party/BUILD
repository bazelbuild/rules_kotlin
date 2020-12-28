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
package(default_visibility = ["//visibility:public"])

load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_plugin")
load("//kotlin:kotlin.bzl", "kt_jvm_import")
load("//kotlin/internal/utils:packager.bzl", "release_archive")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

exports_files([
    "empty.jar",
    "empty.jdeps",
])

java_plugin(
    name = "autovalue_plugin",
    generates_api = 1,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    visibility = ["//visibility:private"],
    deps = ["@kotlin_rules_maven//:com_google_auto_value_auto_value"],
)

java_library(
    name = "autovalue",
    exported_plugins = [":autovalue_plugin"],
    neverlink = 1,
    exports = ["@kotlin_rules_maven//:com_google_auto_value_auto_value_annotations"],
)

java_plugin(
    name = "dagger_component_plugin",
    generates_api = 1,
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    visibility = ["//visibility:private"],
    deps = [
        "@kotlin_rules_maven//:com_google_dagger_dagger_compiler",
    ],
)

java_library(
    name = "dagger",
    exported_plugins = [":dagger_component_plugin"],
    exports = [
        "@kotlin_rules_maven//:com_google_dagger_dagger",
        "@kotlin_rules_maven//:javax_inject_javax_inject",
    ],
)

java_binary(
    name = "jarjar_runner",
    main_class = "org.pantsbuild.jarjar.Main",
    visibility = ["//visibility:public"],
    runtime_deps = ["@kotlin_rules_maven//:org_pantsbuild_jarjar"],
)

# TODO(bazelbuild/rules_kotlin/issues/273): Remove android_sdk import.
java_import(
    name = "android_sdk",
    jars = ["@bazel_tools//tools/android:android_jar"],
    neverlink = 1,
    visibility = ["//visibility:public"],
)

bzl_library(
    name = "bzl",
    srcs = [
        "jarjar.bzl",
        "@bazel_tools//tools:bzl_srcs",
    ],
)

release_archive(
    name = "pkg",
    srcs = [
        "empty.jar",
        "empty.jdeps",
        "jarjar.bzl",
    ],
    src_map = {
        "jarjar_runner_deploy.jar": "jarjar.jar",
        "BUILD.release.bazel": "BUILD.bazel",
    },
)
