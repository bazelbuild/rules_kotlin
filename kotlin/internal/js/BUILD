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

# TODO(https://github.com/bazelbuild/rules_kotlin/issues/270): Replace with Starlark
py_binary(
    name = "importer",
    srcs = ["importer.py"],
    visibility = ["//visibility:public"],
)

release_archive(
    name = "pkg",
    srcs = glob([
        "*.bzl",
        "*.py",
    ]),
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
    },
)
