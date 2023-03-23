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
    "//kotlin/internal:defs.bzl",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
)
load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
    "http_file",
)
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")
load(":compiler.bzl", "kotlin_compiler_repository")
load(":configured_rules.bzl", "rules_repository")
load(":versions.bzl", "version", _versions = "versions")

versions = _versions

RULES_KOTLIN = Label("//:all")

def kotlin_repositories(
        compiler_repository_name = _KT_COMPILER_REPO,
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        configured_repository_name = "io_bazel_rules_kotlin_configured"):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_repository_name: for the kotlinc compiler repository.
        compiler_release: version provider from versions.bzl.
        configured_repository_name: for the default versioned kt_* rules repository. If None, no versioned repository is
         created.
    """

    kotlin_compiler_repository(
        name = compiler_repository_name,
        urls = [url.format(version = compiler_release.version) for url in compiler_release.url_templates],
        sha256 = compiler_release.sha256,
        kotlin_rules = RULES_KOTLIN.workspace_name,
    )

    http_file(
        name = "kt_java_stub_template",
        urls = [("https://raw.githubusercontent.com/bazelbuild/bazel/" +
                 versions.BAZEL_JAVA_LAUNCHER_VERSION +
                 "/src/main/java/com/google/devtools/build/lib/bazel/rules/java/" +
                 "java_stub_template.txt")],
        sha256 = "ab1370fd990a8bff61a83c7bd94746a3401a6d5d2299e54b1b6bc02db4f87f68",
    )

    maybe(
        http_file,
        name = "com_github_pinterest_ktlint",
        sha256 = versions.PINTEREST_KTLINT.sha256,
        urls = [url.format(version = versions.PINTEREST_KTLINT.version) for url in versions.PINTEREST_KTLINT.url_templates],
        executable = True,
    )

    maybe(
        http_archive,
        name = "build_bazel_rules_android",
        sha256 = versions.ANDROID.SHA,
        strip_prefix = "rules_android-%s" % versions.ANDROID.VERSION,
        urls = versions.ANDROID.URLS,
    )

    versions.use_repository(
        name = "rules_python",
        rule = http_archive,
        version = versions.RULES_PYTHON,
    )

    # See note in versions.bzl before updating bazel_skylib
    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (versions.SKYLIB_VERSION, versions.SKYLIB_VERSION)],
        sha256 = versions.SKYLIB_SHA,
    )

    selected_version = None
    for (version, criteria) in versions.CORE.items():
        if (criteria and compiler_release.version.startswith(criteria.prefix)) or (not selected_version and not criteria):
            selected_version = version

    if configured_repository_name and configured_repository_name != "":  # without a repository name, no default kt_* rules repository is created.
        rules_repository(
            name = configured_repository_name,
            archive = Label("//:%s.tgz" % selected_version),
            parent = RULES_KOTLIN,
            repo_mapping = {
                "@dev_io_bazel_rules_kotlin": "@%s" % RULES_KOTLIN.workspace_name,
            },
        )

def kotlinc_version(release, sha256):
    return version(
        version = release,
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = sha256,
    )
