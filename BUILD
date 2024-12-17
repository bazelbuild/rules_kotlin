load("//kotlin:lint.bzl", "ktlint_config")

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

exports_files([
    "scripts/noop.sh",
])

filegroup(
    name = "editorconfig",
    srcs = [".editorconfig"],
)

ktlint_config(
    name = "ktlint_editorconfig",
    android_rules_enabled = False,
    editorconfig = "//:editorconfig",
    experimental_rules_enabled = False,
    visibility = ["//visibility:public"],
)

# The entire test suite excluding local tests.
test_suite(
    name = "all_tests",
    tests = [
        "//src/test/kotlin/io/bazel/kotlin:assertion_tests",
        "//src/test/kotlin/io/bazel/kotlin/builder:builder_tests",
        "//src/test/kotlin/io/bazel/worker:worker_tests",
        "//src/test/starlark:convert_tests",
    ],
)

#  Local tests. Tests that shouldn't be run on the CI server.
test_suite(
    name = "all_local_tests",
    tests = [
        ":all_tests",
        "//src/test/kotlin/io/bazel/kotlin:local_assertion_tests",
        "//src/test/kotlin/io/bazel/worker:local_worker_tests",
        "//src/test/starlark:convert_tests",
    ],
)

# Release target.
release_archive(
    name = "rules_kotlin_release",
    srcs = [
        "WORKSPACE.bzlmod",
    ],
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
        "WORKSPACE.release.bazel": "WORKSPACE",
        "MODULE.release.bazel": "MODULE.bazel",
    },
    deps = [
        "//kotlin:pkg",
        "//src/main/kotlin:pkg",
        "//src/main/starlark:pkg",
        "//third_party:pkg",
    ],
)

# This target collects all of the parent workspace files needed by the child workspaces.
filegroup(
    name = "release_repositories",
    # Include every package that is required by the child workspaces.
    srcs = [
        ":rules_kotlin_release",
    ],
    visibility = ["//:__subpackages__"],
)
