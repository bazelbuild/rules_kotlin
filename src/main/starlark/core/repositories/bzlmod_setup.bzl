# Copyright 2020 The Bazel Authors. All rights reserved.
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
load(":versions.bzl", "version", "versions")
load("//kotlin/internal:defs.bzl", _KT_COMPILER_REPO = "KT_COMPILER_REPO")
load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_file",
)

def _kt_configure_impl(_module_ctx):
    [
        _local_repository(
            name = version,
            path = "src/%s" % version,
        )
        for version in versions.CORE
    ]
    compiler_repository_name = _KT_COMPILER_REPO
    compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE
    kotlin_compiler_repository(
        name = compiler_repository_name,
        urls = [url.format(version = compiler_release.version) for url in compiler_release.url_templates],
        sha256 = compiler_release.sha256,
        kotlin_rules = "dev_io_bazel_rules_kotlin",
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

def _local_repository_impl(repository_ctx):
    path = repository_ctx.attr.path
    root = repository_ctx.path(Label("@//:MODULE.bazel")).dirname
    for segment in path.split("/"):
        root = root.get_child(segment)
    repository_ctx.symlink(root, ".")

_local_repository = repository_rule(
    implementation = _local_repository_impl,
    attrs = {
        "path": attr.string(mandatory = True),
    },
)

kt_bzlmod_ext = module_extension(
    implementation = _kt_configure_impl,
)
