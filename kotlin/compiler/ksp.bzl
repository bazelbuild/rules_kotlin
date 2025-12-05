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

load("@rules_java//java:defs.bzl", "java_binary")
load("//kotlin:jvm.bzl", "kt_jvm_import")
load(
    "//kotlin/internal:defs.bzl",
    _KSP_COMPILER_PLUGIN_REPO = "KSP_COMPILER_PLUGIN_REPO",
)

_KSP_COMPILER_PLUGIN_REPO_PREFIX = "@" + _KSP_COMPILER_PLUGIN_REPO + "//:"

def kt_configure_ksp():
    """
    Defines the toolchain_type and default toolchain for KSP plugins.

    Must be called in kotlin/internal/BUILD.bazel
    """
    if native.package_name() != "kotlin/compiler":
        fail("kt_configure_ksp must be called in kotlin/compiler not %s" % native.package_name())

    kt_jvm_import(
        name = "symbol-processing-aa",
        jar = _KSP_COMPILER_PLUGIN_REPO_PREFIX + "symbol-processing-aa.jar",
    )

    kt_jvm_import(
        name = "symbol-processing-common-deps",
        jar = _KSP_COMPILER_PLUGIN_REPO_PREFIX + "symbol-processing-common-deps.jar",
    )

    kt_jvm_import(
        name = "symbol-processing-api",
        jar = _KSP_COMPILER_PLUGIN_REPO_PREFIX + "symbol-processing-api.jar",
    )

    # KSP2 standalone tool wrapper (kept for backwards compatibility)
    java_binary(
        name = "ksp2_jvm",
        main_class = "com.google.devtools.ksp.cmdline.KSPJvmMain",
        runtime_deps = [
            ":symbol-processing-aa",
            ":symbol-processing-common-deps",
            ":symbol-processing-api",
            ":kotlin-stdlib",
            ":kotlin-stdlib-jdk7",
            ":kotlin-stdlib-jdk8",
            ":kotlinx-coroutines-core-jvm",
        ],
        jvm_flags = ["-Dksp.logging=debug"],
    )
