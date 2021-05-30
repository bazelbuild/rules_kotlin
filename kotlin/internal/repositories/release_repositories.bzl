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
load(":tools.bzl", "absolute_target")
load(":versions.bzl", "versions")

KOTLIN_RULES = absolute_target("//kotlin:kotlin.bzl")

def kotlin_repositories(
        compiler_repostory_name = _KT_COMPILER_REPO,
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) dict containing "urls" and "sha256" for the Kotlin compiler.
    """

    _kotlin_compiler_repository(
        name = _KT_COMPILER_REPO,
        urls = compiler_release["urls"],
        sha256 = compiler_release["sha256"],
        kotlin_rules = KOTLIN_RULES,
    )

    http_file(
        name = "kt_java_stub_template",
        urls = [("https://raw.githubusercontent.com/bazelbuild/bazel/" +
                 versions.BAZEL_JAVA_LAUNCHER_VERSION +
                 "/src/main/java/com/google/devtools/build/lib/bazel/rules/java/" +
                 "java_stub_template.txt")],
        sha256 = "a618e746e743f3119a9939e60645a02de40149aae9d63201c3cd05706010f6eb",
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

def _kotlin_compiler_impl(repository_ctx):
    """Creates the kotlinc repository."""
    attr = repository_ctx.attr

    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        stripPrefix = "kotlinc",
    )
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        substitutions = {
            "{{.KotlinRules}}": attr.kotlin_rules,
        },
        executable = False,
    )

_kotlin_compiler_repository = repository_rule(
    implementation = _kotlin_compiler_impl,
    attrs = {
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "kotlin_rules": attr.string(
            doc = "target of the kotlin rules.",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = Label("//kotlin/internal/repositories:BUILD.com_github_jetbrains_kotlin"),
        ),
    },
)
