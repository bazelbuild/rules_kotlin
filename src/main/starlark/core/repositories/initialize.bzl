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
    ":initialize.release.bzl",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
    _release_kotlin_repositories = "kotlin_repositories",
)
load(":setup.bzl", "kt_configure")
load(":versions.bzl", _versions = "versions")

# exports
versions = _versions
kotlinc_version = _kotlinc_version
ksp_version = _ksp_version

def kotlin_repositories(
        is_bzlmod = False,
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        ksp_compiler_release = versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) version provider from versions.bzl.
        ksp_compiler_release: (internal) version provider from versions.bzl.
    """
    _release_kotlin_repositories(is_bzlmod = is_bzlmod, compiler_release = compiler_release, ksp_compiler_release = ksp_compiler_release)
    kt_configure()
