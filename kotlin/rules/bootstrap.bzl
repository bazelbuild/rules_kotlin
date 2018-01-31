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
"""This file contains rules used to bootstrap the compiler repository."""

load(
    "//kotlin/rules:defs.bzl",
    _KotlinInfo = "KotlinInfo",
)
load(
    "//kotlin/rules:rules.bzl",
    _kotlin_import_impl = "kotlin_import_impl",
)

kotlin_stdlib = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            mandatory = True,
            cfg = "host",
        ),
        "srcjar": attr.label(
            allow_single_file = True,
            cfg = "host",
        ),
    },
    implementation = _kotlin_import_impl,
)

"""Import Kotlin libraries that are part of the compiler release."""
