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
load("//kotlin/internal/utils:packager.bzl", "release_archive")
load("@io_bazel_stardoc//stardoc:stardoc.bzl", "stardoc")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

release_archive(
    name = "pkg",
    srcs = glob(["*.bzl"]),
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
    },
    deps = [
        "//kotlin/internal:pkg",
    ],
)

bzl_library(
    name = "kotlin",
    srcs = ["kotlin.bzl"],
    visibility = ["//kotlin:__subpackages__"],
    deps = [
        "//kotlin/internal",
    ],
)

stardoc(
    name = "stardoc",
    out = "kotlin.md",
    input = "rules.bzl",
    rule_template = "//kotlin:doc-templates/rule.vm",
    symbol_names = [
        "define_kt_toolchain",
        "kt_js_library",
        "kt_js_import",
        "kt_register_toolchains",
        "kt_jvm_binary",
        "kt_jvm_import",
        "kt_jvm_library",
        "kt_jvm_test",
        "kt_android_library",
        "kt_compiler_plugin",
        "kt_kotlinc_options",
        "kt_javac_options",
    ],
    deps = [
        "kotlin",
    ],
)
