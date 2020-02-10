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
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_js_import")

# Create kt_js_imports for KotlinX's Coroutines and Atomicfu libraries.
#
# These could be imported using package.json and NPM, but they are here as an example of how to
# use KotlinJS libraries hosted on Maven directly.
#
# Note: It's important that the `name`s correspond to the base name of the library only (ie. not-ending with -js/_js)
kt_js_import(
    name = "kotlinx-coroutines-core",
    jars = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_js",
    ],
    visibility = ["//:__subpackages__"],
)

kt_js_import(
    name = "kotlinx-atomicfu",
    jars = [
        "@maven//:org_jetbrains_kotlinx_atomicfu_js",
    ],
    visibility = ["//:__subpackages__"],
)
