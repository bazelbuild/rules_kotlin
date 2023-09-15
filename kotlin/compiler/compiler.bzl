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

load("//kotlin:jvm.bzl", "kt_jvm_import")
load("//kotlin:js.bzl", "kt_js_import")
load("@rules_java//java:defs.bzl", "java_import")
load("//kotlin/internal:defs.bzl", _KT_COMPILER_REPO = "KT_COMPILER_REPO")

_KT_COMPILER_REPO_PREFIX = "@" + _KT_COMPILER_REPO + "//:"

def kt_configure_compiler():
    """
    Defines the toolchain_type and default toolchain for kotlin compilation.

    Must be called in kotlin/internal/BUILD.bazel
    """
    if native.package_name() != "kotlin/compiler":
        fail("kt_configure_compiler must be called in kotlin/compiler not %s" % native.package_name())

    kt_jvm_import(
        name = "annotations",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/annotations-13.0.jar",
        neverlink = 1,
    )

    kt_jvm_import(
        name = "jvm-abi-gen",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/jvm-abi-gen.jar",
    )

    # Kotlin dependencies that are internal to this repo and are meant to be loaded manually into a classloader.
    [
        kt_jvm_import(
            name = "kotlin-%s" % art,
            jar = _KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s.jar" % art,
            neverlink = 1,
        )
        for art in [
            "annotation-processing",
            "annotation-processing-runtime",
            "compiler",
        ]
    ]

    kt_jvm_import(
        name = "kotlinx-serialization-compiler-plugin",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/kotlinx-serialization-compiler-plugin.jar",
    )

    kt_jvm_import(
        name = "allopen-compiler-plugin",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/allopen-compiler-plugin.jar",
    )

    kt_jvm_import(
        name = "noarg-compiler-plugin",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/noarg-compiler-plugin.jar",
    )

    kt_jvm_import(
        name = "sam-with-receiver-compiler-plugin",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/sam-with-receiver-compiler-plugin.jar",
    )

    kt_jvm_import(
        name = "parcelize-compiler-plugin",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/parcelize-compiler.jar",
    )

    kt_jvm_import(
        name = "parcelize-runtime",
        jar = _KT_COMPILER_REPO_PREFIX + "lib/parcelize-runtime.jar",
    )

    # Kotlin dependencies that are internal to this repo and may be linked.
    [
        java_import(
            name = "kotlin-%s" % art,
            jars = [_KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s.jar" % art],
        )
        for art in [
            "preloader",
        ]
    ]

    #  The Kotlin standard libraries. These should be setup in a Toolchain.
    [
        kt_jvm_import(
            name = "kotlin-%s" % art,
            jar = _KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s.jar" % art,
            srcjar = _KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s-sources.jar" % art,
            visibility = ["//visibility:public"],
        )
        for art in [
            "stdlib",
            "stdlib-jdk7",
            "stdlib-jdk8",
            "reflect",
            "test",
            "script-runtime",
        ]
    ]

    #  The Kotlin JS standard libraries. These should be setup in a Toolchain.
    [
        kt_js_import(
            name = "kotlin-%s" % art,
            jars = [_KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s.jar" % art],
            srcjar = _KT_COMPILER_REPO_PREFIX + "lib/kotlin-%s-sources.jar" % art,
            visibility = ["//visibility:public"],
        )
        for art in [
            "test-js",
            "stdlib-js",
        ]
    ]
