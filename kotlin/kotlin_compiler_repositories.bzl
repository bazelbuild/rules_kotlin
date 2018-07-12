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
load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file="http_file", _http_archive="http_archive")
load("//kotlin/internal:kt.bzl", _kt = "kt")
load("//kotlin:kotlin_releases.bzl",
    _KOTLIN_COMPILER_RELEASES="KOTLIN_COMPILER_RELEASES",
    _KOTLIN_CURRENT_RELEASE="KOTLIN_CURRENT_RELEASE"
)

_BAZEL_JAVA_LAUNCHER_VERSION = "0.8.1"

def kotlin_compiler_repositories(kotlin_release_version):
    """
    Prime the compiler repository.

    This function should not be called directly instead `kotlin_repositories` from `//kotlin:kotlin.bzl` should be
    called to ensure common deps are loaded.
    """
    release=_KOTLIN_COMPILER_RELEASES[kotlin_release_version]
    if not release:
        fail('"%s" not a valid kotlin release, current release is "%s"' % (kotlin_release_version, _KOTLIN_CURRENT_RELEASE))

    _http_archive(
        name = _kt.defs.KT_COMPILER_REPO,
        url = release["url"],
        sha256 = release["sha256"],
        build_file= "@io_bazel_rules_kotlin//kotlin:BUILD.com_github_jetbrains_kotlin",
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
