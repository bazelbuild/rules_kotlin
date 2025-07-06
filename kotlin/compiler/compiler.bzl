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

load("@com_github_jetbrains_kotlin//:artifacts.bzl", "KOTLINC_ARTIFACTS", "KOTLIN_NATIVE_ARTIFACTS")
load("//kotlin:jvm.bzl", "kt_jvm_import")
load("//kotlin/internal:defs.bzl", _KT_COMPILER_REPO = "KT_COMPILER_REPO", _KT_NATIVE_COMPILER_REPO_PREFIX = "KT_NATIVE_COMPILER_REPO_PREFIX")

KOTLIN_STDLIBS = [
    "//kotlin/compiler:annotations",
    "//kotlin/compiler:kotlin-stdlib",
    "//kotlin/compiler:kotlin-stdlib-jdk7",
    "//kotlin/compiler:kotlin-stdlib-jdk8",
    "//kotlin/compiler:kotlinx-coroutines-core-jvm",
    "//kotlin/compiler:trove4j",
]

def _import_artifacts(artifacts, rule_kind, compiler_repo = _KT_COMPILER_REPO):
    _import_labels(artifacts.plugin, rule_kind, compiler_repo)
    _import_labels(artifacts.runtime, rule_kind, compiler_repo)
    _import_labels(artifacts.compile, rule_kind, compiler_repo, neverlink = 1)

def _import_labels(labels, rule_kind, compiler_repo, **rule_args):
    for (label, file) in labels.items():
        if not file.endswith(".jar"):
            native.filegroup(
                name = label,
                srcs = [
                    "@%s//:%s" % (compiler_repo, label),
                ],
            )
            return

        if "-sources" in label:
            continue
        args = dict(rule_args.items())
        args["visibility"] = ["//visibility:public"]
        args["name"] = label
        args["jars"] = ["@%s//:%s" % (compiler_repo, label)]
        sources = label + "-sources"
        if sources in labels:
            args["srcjar"] = "@%s//:%s" % (compiler_repo, sources)
        rule_kind(**args)

def kt_configure_compiler():
    """
    Defines the toolchain_type and default toolchain for kotlin compilation.

    Must be called in kotlin/internal/BUILD.bazel
    """
    if native.package_name() != "kotlin/compiler":
        fail("kt_configure_compiler must be called in kotlin/compiler not %s" % native.package_name())

    _import_artifacts(KOTLINC_ARTIFACTS.jvm, kt_jvm_import)
    _import_artifacts(KOTLINC_ARTIFACTS.core, kt_jvm_import)
    _import_artifacts(KOTLIN_NATIVE_ARTIFACTS.linux_x86_64, kt_jvm_import, compiler_repo = _KT_NATIVE_COMPILER_REPO_PREFIX + "_linux_x86_64")
    _import_artifacts(KOTLIN_NATIVE_ARTIFACTS.macos_x86_64, kt_jvm_import, compiler_repo = _KT_NATIVE_COMPILER_REPO_PREFIX + "_macos_x86_64")
    _import_artifacts(KOTLIN_NATIVE_ARTIFACTS.macos_aarch64, kt_jvm_import, compiler_repo = _KT_NATIVE_COMPILER_REPO_PREFIX + "_macos_aarch64")
    _import_artifacts(KOTLIN_NATIVE_ARTIFACTS.windows_x86_64, kt_jvm_import, compiler_repo = _KT_NATIVE_COMPILER_REPO_PREFIX + "_windows_x86_64")

    # a convenience alias for kotlin-native to be referenced in other places
    native.alias(
        actual = select({
            "@bazel_tools//src/conditions:linux_x86_64": "//kotlin/compiler:kotlin-native-linux-x86_64",
            "@bazel_tools//src/conditions:darwin": "//kotlin/compiler:kotlin-native-macos-x86_64",
            "@bazel_tools//src/conditions:windows": "//kotlin/compiler:kotlin-native-windows_x86_64",
            "@bazel_tools//src/conditions:darwin_arm64": "//kotlin/compiler:kotlin-native-macos_aarch64",
        }),
        name = "kotlin-native",
    )
