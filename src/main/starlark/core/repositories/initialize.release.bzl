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
    "http_jar",
)
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")
load(
    "//kotlin/internal:defs.bzl",
    _KSP_COMPILER_PLUGIN_REPO = "KSP_COMPILER_PLUGIN_REPO",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
)
load(":compiler.bzl", "kotlin_compiler_repository")
load(":ksp.bzl", "ksp_compiler_plugin_repository")
load(":versions.bzl", "version", _versions = "versions")

versions = _versions

RULES_KOTLIN = Label("//:all")

def kotlin_repositories(
        is_bzlmod = False,
        compiler_repository_name = _KT_COMPILER_REPO,
        ksp_repository_name = _KSP_COMPILER_PLUGIN_REPO,
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        ksp_compiler_release = versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_repository_name: for the kotlinc compiler repository.
        compiler_release: version provider from versions.bzl.
        configured_repository_name: for the default versioned kt_* rules repository. If None, no versioned repository is
         created.
        ksp_compiler_release: (internal) version provider from versions.bzl.
    """

    kotlin_compiler_repository(
        name = compiler_repository_name,
        urls = [url.format(version = compiler_release.version) for url in compiler_release.url_templates],
        sha256 = compiler_release.sha256,
        compiler_version = compiler_release.version,
    )

    ksp_compiler_plugin_repository(
        name = ksp_repository_name,
        urls = [url.format(version = ksp_compiler_release.version) for url in ksp_compiler_release.url_templates],
        sha256 = ksp_compiler_release.sha256,
        strip_version = ksp_compiler_release.version,
    )

    maybe(
        http_file,
        name = "com_github_pinterest_ktlint",
        sha256 = versions.PINTEREST_KTLINT.sha256,
        urls = [url.format(version = versions.PINTEREST_KTLINT.version) for url in versions.PINTEREST_KTLINT.url_templates],
        executable = True,
    )

    maybe(
        http_jar,
        name = "kotlinx_serialization_core_jvm",
        sha256 = versions.KOTLINX_SERIALIZATION_CORE_JVM.sha256,
        urls = [url.format(version = versions.KOTLINX_SERIALIZATION_CORE_JVM.version) for url in versions.KOTLINX_SERIALIZATION_CORE_JVM.url_templates],
    )

    maybe(
        http_jar,
        name = "kotlinx_serialization_json",
        sha256 = versions.KOTLINX_SERIALIZATION_JSON.sha256,
        urls = [url.format(version = versions.KOTLINX_SERIALIZATION_JSON.version) for url in versions.KOTLINX_SERIALIZATION_JSON.url_templates],
    )

    maybe(
        http_jar,
        name = "kotlinx_serialization_json_jvm",
        sha256 = versions.KOTLINX_SERIALIZATION_JSON_JVM.sha256,
        urls = [url.format(version = versions.KOTLINX_SERIALIZATION_JSON_JVM.version) for url in versions.KOTLINX_SERIALIZATION_JSON_JVM.url_templates],
    )

    if is_bzlmod:
        return

    maybe(
        http_archive,
        name = "py_absl",
        sha256 = "8a3d0830e4eb4f66c4fa907c06edf6ce1c719ced811a12e26d9d3162f8471758",
        urls = [
            "https://github.com/abseil/abseil-py/archive/refs/tags/v2.1.0.tar.gz",
        ],
        strip_prefix = "abseil-py-2.1.0",
    )

    maybe(
        http_archive,
        name = "rules_cc",
        urls = ["https://github.com/bazelbuild/rules_cc/releases/download/0.0.16/rules_cc-0.0.16.tar.gz"],
        sha256 = "bbf1ae2f83305b7053b11e4467d317a7ba3517a12cef608543c1b1c5bf48a4df",
        strip_prefix = "rules_cc-0.0.16",
    )

    maybe(
        http_archive,
        name = "rules_license",
        sha256 = versions.RULES_LICENSE.sha256,
        urls = [url.format(version = versions.RULES_LICENSE.version) for url in versions.RULES_LICENSE.url_templates],
    )

    maybe(
        http_archive,
        name = "rules_android",
        sha256 = versions.RULES_ANDROID.sha256,
        urls = [url.format(version = versions.RULES_ANDROID.version) for url in versions.RULES_ANDROID.url_templates],
    )

    maybe(
        http_archive,
        name = "rules_java",
        sha256 = versions.RULES_JAVA.sha256,
        urls = [url.format(version = versions.RULES_JAVA.version) for url in versions.RULES_JAVA.url_templates],
    )

    # See note in versions.bzl before updating bazel_skylib
    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (versions.SKYLIB_VERSION, versions.SKYLIB_VERSION)],
        sha256 = versions.SKYLIB_SHA,
    )

    PROTOBUF_VERSION = "29.0"
    PROTOBUF_HASH = "10a0d58f39a1a909e95e00e8ba0b5b1dc64d02997f741151953a2b3659f6e78c"
    maybe(
        http_archive,
        name = "com_google_protobuf",
        sha256 = PROTOBUF_HASH,
        strip_prefix = "protobuf-" + PROTOBUF_VERSION,
        urls = ["https://github.com/protocolbuffers/protobuf/releases/download/v{0}/protobuf-{0}.tar.gz".format(PROTOBUF_VERSION)],
    )

    maybe(
        http_archive,
        name = "rules_proto",
        sha256 = "0e5c64a2599a6e26c6a03d6162242d231ecc0de219534c38cb4402171def21e8",
        strip_prefix = "rules_proto-7.0.2",
        url = "https://github.com/bazelbuild/rules_proto/releases/download/7.0.2/rules_proto-7.0.2.tar.gz",
    )

def kotlinc_version(release, sha256):
    return version(
        version = release,
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = sha256,
    )

def ksp_version(release, sha256):
    return version(
        version = release,
        url_templates = [
            "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
        ],
        sha256 = sha256,
    )
