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
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_compiler_plugin", "kt_ksp_plugin_info")
load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_plugin")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

# Kotlin Toolchain

define_kt_toolchain(name = "kotlin_toolchain")

#java_plugin(
#    name = "moshi-kotlin-codegen",
#    features = ["ksp"],
#    generates_api = 1,
#    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
#    deps = [
#        "@maven//:com_squareup_moshi_moshi",
#        "@maven//:com_squareup_moshi_moshi_kotlin",
#        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
#    ],
#)

kt_ksp_plugin_info(
    name = "moshi-kotlin-codegen",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

#kt_jvm_library(
#    name = "moshi-java-plugin",
#    exported_compiler_plugins = [":moshi-kotlin-codegen"],
#    exports = [
#        "@maven//:com_squareup_moshi_moshi",
#        "@maven//:com_squareup_moshi_moshi_kotlin",
#    ],
#)

kt_jvm_library(
    name = "coffee_lib",
    srcs = glob(["*.kt"]),
    plugins = ["//:moshi-kotlin-codegen"],
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

java_binary(
    name = "coffee_app",
    main_class = "coffee.CoffeeApp",
    visibility = ["//visibility:public"],
    runtime_deps = [":coffee_lib"],
)

build_test(
    name = "force_build_app_test",
    targets = [
        "//:coffee_app",
    ],
)
