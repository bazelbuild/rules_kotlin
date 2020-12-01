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
load("@rules_java//java:defs.bzl", "java_test")
load("//kotlin:kotlin.bzl", "kt_jvm_test")

def _get_class_name(kwargs):
    if len(kwargs.get("srcs", [])) == 1:
        return (
            native.package_name().replace("src/test/kotlin/", "") +
            "." + kwargs["srcs"][0]
        ).replace("/", ".").replace(".java", "").replace(".kt", "")
    else:
        return kwargs["test_classes"]

def kt_rules_test(name, **kwargs):
    args = dict(kwargs.items())
    args.setdefault("size", "small")
    args.setdefault("data", [])
    args.setdefault("jvm_flags", [])
    args["deps"] = args.setdefault("deps", []) + ["//src/test/kotlin/io/bazel/kotlin/builder:test_lib"]
    for dep in [
        "//src/main/kotlin/io/bazel/kotlin/compiler",
        "//src/main/kotlin:skip-code-gen",
        "//src/main/kotlin:jdeps-gen",
        "@com_github_jetbrains_kotlin//:annotations",
        "@com_github_jetbrains_kotlin//:jvm-abi-gen",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
    ] + args["data"]:
        if dep not in args["data"]:
            args["data"] += [dep]
        args["jvm_flags"] += ["-D%s=$(rootpath %s)" % (dep.replace("/", ".").replace(":", "."), dep)]

    args.setdefault("test_class", _get_class_name(kwargs))
    for f in args.get("srcs"):
        if f.endswith(".kt"):
            kt_jvm_test(name = name, **args)
            return
    java_test(name = name, **args)

def kt_rules_e2e_test(name, **kwargs):
    kwargs.setdefault("size", "small")
    kwargs["deps"] = kwargs.setdefault("deps", []) + ["//src/test/kotlin/io/bazel/kotlin:assertion_test_case"]
    kwargs.setdefault("test_class", _get_class_name(kwargs))
    kt_jvm_test(name = name, **kwargs)
