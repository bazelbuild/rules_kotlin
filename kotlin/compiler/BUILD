# Copyright 2020 The Bazel Authors. All rights reserved.
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

load("@rules_java//java:java_library.bzl", "java_library")
load("//src/main/starlark/release:packager.bzl", "release_archive")
load(":kapt.bzl", "kapt_compiler_plugin")

package(default_visibility = ["//visibility:public"])

# Kotlin standard library
alias(
    name = "kotlin-stdlib",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_stdlib",
)

alias(
    name = "kotlin-reflect",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_reflect",
)

alias(
    name = "kotlin-test",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_test",
)

alias(
    name = "annotations",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_annotations",
)

alias(
    name = "kotlinx-coroutines-core-jvm",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
)

java_library(
    name = "kotlin-annotation-processing",
    neverlink = True,
    exports = ["@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_annotation_processing"],
)

alias(
    name = "jvm-abi-gen",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_jvm_abi_gen",
)

alias(
    name = "kotlin-script-runtime",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_script_runtime",
)

# Compiler plugins
alias(
    name = "allopen-compiler-plugin",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_allopen_compiler_plugin",
)

alias(
    name = "noarg-compiler-plugin",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_noarg_compiler_plugin",
)

alias(
    name = "sam-with-receiver-compiler-plugin",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_sam_with_receiver_compiler_plugin",
)

alias(
    name = "kotlinx-serialization-compiler-plugin",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_serialization_compiler_plugin",
)

alias(
    name = "kotlin-serialization-compiler-plugin",
    actual = ":kotlinx-serialization-compiler-plugin",
)

alias(
    name = "parcelize-compiler-plugin",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_parcelize_compiler",
)

alias(
    name = "parcelize-runtime",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_parcelize_runtime",
)

alias(
    name = "kotlin-daemon-client",
    actual = "@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_daemon_client",
)

alias(
    name = "kotlin-build-tools-impl",
    actual = "@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_build_tools_impl",
)

alias(
    name = "kotlin-compiler",
    actual = "@rules_kotlin_dev_maven//:org_jetbrains_kotlin_kotlin_compiler",
)

alias(
    name = "kotlin-compiler-embeddable",
    actual = "@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_compiler_embeddable",
)

alias(
    name = "kotlin-annotation-processing-embeddable",
    actual = "@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_annotation_processing_embeddable",
)

alias(
    name = "symbol-processing-aa",
    actual = "@rules_kotlin_maven//:com_google_devtools_ksp_symbol_processing_aa",
)

kapt_compiler_plugin(
    name = "kapt",
    id = "org.jetbrains.kotlin.kapt3",
)

release_archive(
    name = "pkg",
    srcs = glob(["*.bzl"]),
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
    },
)
