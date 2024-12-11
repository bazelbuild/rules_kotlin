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
        name = "rules_android",
        sha256 = versions.ANDROID.SHA,
        strip_prefix = "rules_android-%s" % versions.ANDROID.VERSION,
        urls = versions.ANDROID.URLS,
    )

    versions.use_repository(
        name = "rules_java",
        rule = http_archive,
        version = versions.RULES_JAVA,
    )

    # See note in versions.bzl before updating bazel_skylib
    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (versions.SKYLIB_VERSION, versions.SKYLIB_VERSION)],
        sha256 = versions.SKYLIB_SHA,
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
