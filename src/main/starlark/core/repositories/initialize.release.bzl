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

KOTLIN_RULES = Label("//:all")

def kotlin_repositories(
        compiler_repository_name = _KT_COMPILER_REPO,
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        configured_repository_name = "io_bazel_rules_kotlin_configured"):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) version provider from versions.bzl.
    """

    kotlin_compiler_repository(
        name = _KT_COMPILER_REPO,
        urls = [url.format(version = compiler_release.version) for url in compiler_release.url_templates],
        sha256 = compiler_release.sha256,
        kotlin_rules = KOTLIN_RULES.workspace_name,
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
        sha256 = "4739662e9ac9a9894a1eb47844cbb5610971f15af332eac94d108d4f55ebc19e",
        urls = ["https://github.com/pinterest/ktlint/releases/download/0.40.0/ktlint"],
        executable = True,
    )

    maybe(
        http_archive,
        name = "rules_android",
        sha256 = versions.ANDROID.SHA,
        strip_prefix = "rules_android-%s" % versions.ANDROID.VERSION,
        urls = versions.ANDROID.URLS,
    )

    maybe(
        http_archive,
        name = "rules_python",
        sha256 = "778197e26c5fbeb07ac2a2c5ae405b30f6cb7ad1f5510ea6fdac03bded96cc6f",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_python/releases/download/{version}/rules_python-{version}.tar.gz".format(
                version = versions.PYTHON.VERSION,
            ),
            "https://github.com/bazelbuild/rules_python/releases/download/{version}/rules_python-{version}.tar.gz".format(
                version = versions.PYTHON.VERSION,
            ),
        ],
    )

    selected_version = None
    for (version, criteria) in versions.CORE.items():
        if (criteria and compiler_release.version.startswith(criteria.prefix)) or (not selected_version and not criteria):
            selected_version = version

    rules_repository(
        name = configured_repository_name,
        archive = Label("//:%s.tgz" % selected_version),
        parent = KOTLIN_RULES,
        repo_mapping = {
            "@dev_io_bazel_rules_kotlin": "@%s" % KOTLIN_RULES.workspace_name,
            "@": "@%s" % KOTLIN_RULES.workspace_name,
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
