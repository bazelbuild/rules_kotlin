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
load("@rules_java//java:defs.bzl", "java_plugin")
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("@bazel_skylib//rules:build_test.bzl", "build_test")

package(default_visibility = ["//visibility:public"])

# Kotlin Toolchain

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.6",
    experimental_annotation_processing_mode = "ksp",
    experimental_use_abi_jars = True,
    jvm_target = "1.8",
    language_version = "1.6",
)

# Moshi compiler plugin

java_plugin(
    name = "moshi-kotlin-codegen",
    generates_api = 1,
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

java_library(
    name = "moshi-kotlin-codegen-lib",
    exported_plugins = [":moshi-kotlin-codegen"],
    exports = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

build_test(
    name = "force_build_app_test",
    targets = [
        "//app:coffee_app",
    ],
)
