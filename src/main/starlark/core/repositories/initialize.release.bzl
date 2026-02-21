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
"""This file contains the Kotlin compiler repository definitions. It should not be loaded directly by client workspaces.
"""

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
    "http_file",
)
load(
    "//kotlin/internal:defs.bzl",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
)
load(":compiler.bzl", "kotlin_capabilities_repository")
load(":versions.bzl", "version", _versions = "versions")

versions = _versions

RULES_KOTLIN = Label("//:all")

def kotlin_repositories(
        is_bzlmod = False,
        compiler_repository_name = _KT_COMPILER_REPO,
        compiler_version = versions.KOTLIN_CURRENT_COMPILER_VERSION):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_repository_name: for the kotlinc compiler repository.
        compiler_version: Kotlin compiler version string (e.g. "2.3.20-RC").
    """

    kotlin_capabilities_repository(
        name = compiler_repository_name,
        compiler_version = compiler_version,
    )

    versions.use_repository(
        http_file,
        name = "com_github_pinterest_ktlint",
        version = versions.PINTEREST_KTLINT,
        downloaded_file_path = "ktlint.jar",
    )

    if is_bzlmod:
        return

    versions.use_repository(
        http_archive,
        name = "py_absl",
        version = versions.PY_ABSL,
    )

    versions.use_repository(
        http_archive,
        name = "py_absl",
        version = versions.PY_ABSL,
    )

    versions.use_repository(
        http_archive,
        name = "rules_cc",
        version = versions.RULES_CC,
    )
    versions.use_repository(
        http_archive,
        name = "rules_license",
        version = versions.RULES_LICENSE,
    )
    versions.use_repository(
        http_archive,
        name = "rules_android",
        version = versions.RULES_ANDROID,
    )

    versions.use_repository(
        http_archive,
        name = "rules_java",
        version = versions.RULES_JAVA,
    )

    # See note in versions.bzl before updating bazel_skylib
    versions.use_repository(
        http_archive,
        name = "bazel_skylib",
        version = versions.BAZEL_SKYLIB,
    )

    versions.use_repository(
        http_archive,
        name = "bazel_features",
        version = versions.BAZEL_FEATURES,
    )

    versions.use_repository(
        http_archive,
        name = "bazel_lib",
        version = versions.BAZEL_LIB,
    )

    versions.use_repository(
        http_archive,
        name = "com_google_protobuf",
        version = versions.COM_GOOGLE_PROTOBUF,
    )
    versions.use_repository(
        http_archive,
        name = "rules_proto",
        version = versions.RULES_PROTO,
    )

def kotlinc_version(release, sha256):
    return version(
        version = release,
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = sha256,
    )
