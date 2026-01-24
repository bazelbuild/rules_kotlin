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

load("//src/main/starlark/release:packager.bzl", "release_archive")
load("//kotlin:jvm.bzl", "kt_jvm_import")
load(":kapt.bzl", "kapt_compiler_plugin")
load(":ksp.bzl", "kt_configure_ksp")

package(default_visibility = ["//visibility:public"])

# Configures the KSP plugins
kt_configure_ksp()

# Kotlin standard library
kt_jvm_import(
    name = "kotlin-stdlib",
    jar = "@kotlin_stdlib//file",
)

# For backward compatibility - in Kotlin 1.8+ JDK extensions are in main stdlib
alias(
    name = "kotlin-stdlib-jdk7",
    actual = ":kotlin-stdlib",
)

alias(
    name = "kotlin-stdlib-jdk8",
    actual = ":kotlin-stdlib",
)

kt_jvm_import(
    name = "kotlin-reflect",
    jar = "@kotlin_reflect//file",
)

kt_jvm_import(
    name = "kotlin-test",
    jar = "@kotlin_test//file",
)

kt_jvm_import(
    name = "annotations",
    jar = "@jetbrains_annotations//file",
)

kt_jvm_import(
    name = "kotlinx-coroutines-core-jvm",
    jar = "@kotlinx_coroutines_core_jvm//file",
)

# Kotlin compiler
kt_jvm_import(
    name = "kotlin-compiler",
    jar = "@kotlin_compiler//file",
    neverlink = 1,
)

kt_jvm_import(
    name = "kotlin-annotation-processing",
    jar = "@kotlin_annotation_processing//file",
    neverlink = 1,
)

kt_jvm_import(
    name = "jvm-abi-gen",
    jar = "@kotlin_jvm_abi_gen//file",
)

kt_jvm_import(
    name = "kotlin-script-runtime",
    jar = "@kotlin_script_runtime//file",
)

# Compiler plugins
kt_jvm_import(
    name = "allopen-compiler-plugin",
    jar = "@kotlin_allopen_compiler_plugin//file",
)

kt_jvm_import(
    name = "noarg-compiler-plugin",
    jar = "@kotlin_noarg_compiler_plugin//file",
)

kt_jvm_import(
    name = "sam-with-receiver-compiler-plugin",
    jar = "@kotlin_sam_with_receiver_compiler_plugin//file",
)

kt_jvm_import(
    name = "kotlin-serialization-compiler-plugin",
    jar = "@kotlin_serialization_compiler_plugin//file",
)

# Alias for backward compatibility
alias(
    name = "kotlinx-serialization-compiler-plugin",
    actual = ":kotlin-serialization-compiler-plugin",
)

kt_jvm_import(
    name = "parcelize-compiler-plugin",
    jar = "@kotlin_parcelize_compiler//file",
)

kt_jvm_import(
    name = "parcelize-runtime",
    jar = "@kotlin_parcelize_runtime//file",
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
