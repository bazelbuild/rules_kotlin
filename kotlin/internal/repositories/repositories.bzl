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
    _http_archive = "http_archive",
    _http_file = "http_file",
)
load(
    "//kotlin/internal:defs.bzl",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
)
load(
    "//third_party/jvm:workspace.bzl",
    _maven_dependencies = "maven_dependencies",
)
load(
    "//kotlin/internal/repositories:nomaven_repositories.bzl",
    "KOTLIN_CURRENT_COMPILER_RELEASE",
    _kotlin_repositories_no_maven = "kotlin_repositories",
)

def github_archive(name, repo, commit, build_file_content = None, sha256 = None):
    if build_file_content:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
            build_file_content = build_file_content,
            sha256 = sha256,
        )
    else:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
            sha256 = sha256,
        )

def kotlin_repositories(compiler_release = KOTLIN_CURRENT_COMPILER_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) dict containing "urls" and "sha256" for the Kotlin compiler.
    """
    _maven_dependencies()
    _kotlin_repositories_no_maven(compiler_release)
