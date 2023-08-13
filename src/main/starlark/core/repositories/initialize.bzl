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

load(":setup.bzl", "kt_configure")
load(
    ":initialize.release.bzl",
    _RULES_KOTLIN = "RULES_KOTLIN",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
    _release_kotlin_repositories = "kotlin_repositories",
)
load("//src/main/starlark/core/repositories/kotlin:compiler.bzl", "kotlin_compiler_repository")
load("//src/main/starlark/core/repositories/kotlin:releases.bzl", "KOTLINC_INDEX")
load(":versions.bzl", _versions = "versions")

#exports
versions = _versions
kotlinc_version = _kotlinc_version
ksp_version = _ksp_version

def kotlin_repositories(
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        ksp_compiler_release = versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) version provider from versions.bzl.
        ksp_compiler_release: (internal) version provider from versions.bzl.
    """
    _release_kotlin_repositories(compiler_release = compiler_release, ksp_compiler_release = ksp_compiler_release)
    kt_configure()

    # Provide versioned kotlinc repositories. These are used for compiling plugins.
    for versioned_kotlinc in KOTLINC_INDEX.values():
        kotlin_compiler_repository(
            name = versioned_kotlinc.repository_name,
            urls = [
                url.format(version = versioned_kotlinc.release.version)
                for url in versioned_kotlinc.release.url_templates
            ],
            sha256 = versioned_kotlinc.release.sha256,
            kotlin_rules = _RULES_KOTLIN.workspace_name,
            compiler_version = versioned_kotlinc.release.version,
        )
