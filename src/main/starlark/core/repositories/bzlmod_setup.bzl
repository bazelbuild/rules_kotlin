# Copyright 2022 The Bazel Authors. All rights reserved.
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

load(":compiler.bzl", "kotlin_compiler_repository")
load(":ksp.bzl", "ksp_compiler_plugin_repository")
load(":versions.bzl", "versions")
load("//kotlin/internal:defs.bzl", _KSP_COMPILER_PLUGIN_REPO = "KSP_COMPILER_PLUGIN_REPO", _KT_COMPILER_REPO = "KT_COMPILER_REPO")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

def _kt_configure_impl(_module_ctx):
    # TODO: release compiler repository on bazel-central-registry and remove it from here
    compiler_repository_name = _KT_COMPILER_REPO
    compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE
    kotlin_compiler_repository(
        name = compiler_repository_name,
        urls = [url.format(version = compiler_release.version) for url in compiler_release.url_templates],
        sha256 = compiler_release.sha256,
        compiler_version = compiler_release.version,
    )

    ksp_repository_name = _KSP_COMPILER_PLUGIN_REPO
    ksp_compiler_release = versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE
    ksp_compiler_plugin_repository(
        name = ksp_repository_name,
        urls = [url.format(version = ksp_compiler_release.version) for url in ksp_compiler_release.url_templates],
        sha256 = ksp_compiler_release.sha256,
        strip_version = ksp_compiler_release.version,
    )

    http_file(
        name = "kt_java_stub_template",
        urls = [("https://raw.githubusercontent.com/bazelbuild/bazel/" +
                 versions.BAZEL_JAVA_LAUNCHER_VERSION +
                 "/src/main/java/com/google/devtools/build/lib/bazel/rules/java/" +
                 "java_stub_template.txt")],
        sha256 = "ab1370fd990a8bff61a83c7bd94746a3401a6d5d2299e54b1b6bc02db4f87f68",
    )

    http_file(
        name = "com_github_pinterest_ktlint",
        sha256 = versions.PINTEREST_KTLINT.sha256,
        urls = [url.format(version = versions.PINTEREST_KTLINT.version) for url in versions.PINTEREST_KTLINT.url_templates],
        executable = True,
    )

    # This tarball intentionally does not have a SHA256 because the upstream URL can change without notice
    # For more context: https://github.com/bazelbuild/bazel-toolchains/blob/0c1f7c3c5f9e63f1e0ee91738b964937eea2d3e0/WORKSPACE#L28-L32
    http_file(
        name = "buildkite_config",
        urls = versions.RBE.URLS,
    )

kt_bzlmod_ext = module_extension(
    implementation = _kt_configure_impl,
)
