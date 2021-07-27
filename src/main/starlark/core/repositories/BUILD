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
load("//src/main/starlark/release:packager.bzl", "release_archive")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

release_archive(
    name = "pkg",
    srcs = [
        "BUILD.com_github_jetbrains_kotlin.bazel",
        "compiler.bzl",
        "configured_rules.bzl",
        "tools.bzl",
        "versions.bzl",
    ],
    src_map = {
        "initialize.release.bzl": "initialize.bzl",
        "BUILD.release.bazel": "BUILD.bazel",
    },
)

bzl_library(
    name = "repositories",
    srcs = glob(["*.bzl"]),
    visibility = ["//:__subpackages__"],
    deps = [
        "@rules_proto//proto",
        "@rules_proto//proto/private",
    ],
)
