# RELEASE-CONTENT-START
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
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "kotlin",
    srcs = glob(
        ["*.bzl"],
        exclude = [
            "android.bzl",
            "kotlin.bzl",
        ],
    ),
    visibility = ["//visibility:public"],
    deps = [
        "//kotlin/internal",
    ],
)
# RELEASE-CONTENT-END

# Dev-only: exports for documentation
exports_files([
    "jvm.bzl",
    "lint.bzl",
    "core.bzl",
    "repositories.doc.bzl",
])

# Dev-only: packaging rule
load("//src/main/starlark/release:packager.bzl", "release_archive")

release_archive(
    name = "pkg",
    srcs = glob(
        ["*.bzl"],
        exclude = [
            "repositories.doc.bzl",
        ],
    ),
    src_map = {
        "BUILD": "BUILD.bazel",
    },
    deps = [
        "//kotlin/compiler:pkg",
        "//kotlin/internal:pkg",
        "//kotlin/settings:pkg",
    ],
)
