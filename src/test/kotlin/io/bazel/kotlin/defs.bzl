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
    kwargs.setdefault("size", "small")
    kwargs["deps"] = kwargs.setdefault("deps", []) + ["//src/test/kotlin/io/bazel/kotlin/builder:test_lib"]
    kwargs.setdefault("test_class", _get_class_name(kwargs))
    for f in kwargs.get("srcs"):
        if f.endswith(".kt"):
            kt_jvm_test(name = name, **kwargs)
            return
    native.java_test(name = name, **kwargs)

def kt_rules_e2e_test(name, **kwargs):
    kwargs.setdefault("size", "small")
    kwargs["deps"] = kwargs.setdefault("deps", []) + ["//src/test/kotlin/io/bazel/kotlin:assertion_test_case"]
    kwargs.setdefault("test_class", _get_class_name(kwargs))
    kt_jvm_test(name = name, **kwargs)
