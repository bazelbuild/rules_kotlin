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
load("@released_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_java//java:defs.bzl", "java_binary", "java_import")
load("//third_party:jarjar.bzl", "jar_jar")

_BOOTSTRAP_LIB_ARGS = ["-jvm-target", "1.8"]

def kt_bootstrap_library(name, deps = [], neverlink_deps = [], **kwargs):
    """
    Simple compilation of a kotlin library using a non-persistent worker. The target is a JavaInfo provider.

    The target is tagged `"no-ide"` as intellij can't compile it. A seperate private target is created which is suffixed
    with `_for_ide`. If the dep is under the package `//src/main/kotlin/io/bazel/kotlin/builder/...` then it will be
    added to the `_for_ide` target by adding a `_for_ide` prefix.

    deps: the dependenices, the are setup as runtime_deps of the library.
    neverlink_deps: deps that won't be linked. These deps are added to the `"for_ide"` target.
    """
    kt_jvm_library(
        name = "%s_neverlink" % name,
        exports = neverlink_deps,
        neverlink = True,
    )

    kt_jvm_library(
        name = name,
        deps = deps + ["%s_neverlink" % name],
        **kwargs
    )

def kt_bootstrap_binary(
        name,
        main_class,
        runtime_library,
        shade_rules,
        jvm_flags = [],
        data = [],
        visibility = ["//visibility:public"]):
    raw = name + "_raw"
    jar_jared = name + "_jarjar"

    java_binary(
        name = raw,
        create_executable = False,
        runtime_deps = [runtime_library],
    )

    # Shaded to ensure that libraries it uses are not leaked to
    # the code it's running against (e.g. dagger)
    jar_jar(
        name = jar_jared,
        input_jar = ":" + raw + "_deploy.jar",
        rules = shade_rules,
    )

    java_binary(
        name = name,
        data = data,
        jvm_flags = jvm_flags + [
            "-XX:+IgnoreUnrecognizedVMOptions",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-opens=jdk.jdeps/com.sun.tools.jdeps=ALL-UNNAMED",
        ],
        main_class = main_class,
        visibility = visibility,
        runtime_deps = [":" + jar_jared],
    )
