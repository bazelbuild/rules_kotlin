load("@rules_java//java:java_binary.bzl", "java_binary")
load("//src/main/starlark/release:packager.bzl", "release_archive")

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
load(":compiler.bzl", "kt_configure_compiler")
load(":kapt.bzl", "kapt_compiler_plugin")
load(":ksp.bzl", "kt_configure_ksp")

package(default_visibility = ["//visibility:public"])

# Configures the compiler
kt_configure_compiler()

# Configures the KSP plugins
kt_configure_ksp()

release_archive(
    name = "pkg",
    srcs = glob(["*.bzl"]),
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
    },
)

kapt_compiler_plugin(
    name = "kapt",
    id = "org.jetbrains.kotlin.kapt3",
)

# KSP2 Standalone binary
# Runs KSP2 in standalone mode (not as a compiler plugin)
# Uses the Analysis API-based implementation
java_binary(
    name = "ksp2_standalone",
    main_class = "com.google.devtools.ksp.cmdline.KSPJvmMain",
    runtime_deps = [
        ":kotlin-stdlib",
        "@kotlin_rules_maven//:com_google_devtools_ksp_symbol_processing_aa",
        "@kotlin_rules_maven//:com_google_devtools_ksp_symbol_processing_common_deps",
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)
