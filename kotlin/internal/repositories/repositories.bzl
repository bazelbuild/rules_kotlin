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

load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive", _http_file = "http_file")
load("//kotlin/internal:defs.bzl", "KT_COMPILER_REPO")
load("//kotlin/internal/repositories:compiler_releases.bzl", "KOTLIN_COMPILER_RELEASES", "KOTLIN_CURRENT_RELEASE")
load("//third_party/jvm:workspace.bzl", _maven_dependencies = "maven_dependencies")

_BAZEL_JAVA_LAUNCHER_VERSION = "0.8.1"

def github_archive(name, repo, commit, build_file_content = None):
    if build_file_content:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
            build_file_content = build_file_content,
        )
    else:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
        )

def _compiler_repositories(kotlin_release_version):
    """
    Prime the compiler repository.

    This function should not be called directly instead `kotlin_repositories` from `//kotlin:kotlin.bzl` should be
    called to ensure common deps are loaded.
    """
    release = KOTLIN_COMPILER_RELEASES[kotlin_release_version]
    if not release:
        fail('"%s" not a valid kotlin release, current release is "%s"' % (kotlin_release_version, KOTLIN_CURRENT_RELEASE))

    _http_archive(
        name = KT_COMPILER_REPO,
        url = release["url"],
        sha256 = release["sha256"],
        build_file = "@io_bazel_rules_kotlin//kotlin/internal/repositories:BUILD.com_github_jetbrains_kotlin",
        strip_prefix = "kotlinc",
    )

    _http_file(
        name = "kt_java_stub_template",
        urls = [("https://raw.githubusercontent.com/bazelbuild/bazel/" +
                 _BAZEL_JAVA_LAUNCHER_VERSION +
                 "/src/main/java/com/google/devtools/build/lib/bazel/rules/java/" +
                 "java_stub_template.txt")],
        sha256 = "86660ee7d5b498ccf611a1e000564f45268dbf301e0b2b08c984dcecc6513f6e",
    )

def kotlin_repositories(
        kotlin_release_version = KOTLIN_CURRENT_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
      kotlin_release_version: The kotlin compiler release version. If this is not set the latest release version is
      chosen by default.
    """
    _maven_dependencies()
    _compiler_repositories(kotlin_release_version)
