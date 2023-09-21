load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary")

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
load("@rules_kotlin//kotlin:js.bzl", "kt_js_library")

# routes and auth have acme prepended to the rule name as these are the names of the resulting jars. The name of the
# jar is the name of the module, and thus the name of the require statements.
kt_js_library(
    name = "acme-routes",
    srcs = [":Routes.kt"],
    deps = [
        "//:kotlinx-atomicfu",
        "//:kotlinx-coroutines-core",
        "//express/auth:acme-auth",
    ],
)

kt_js_library(
    name = "app",
    srcs = [":App.kt"],
    tags = ["trace", "timings"],
    deps = [
        ":acme-routes",
        "//:kotlinx-coroutines-core",
    ],
)

# The binary demonstrates an express app composed of three modules. The modules can co-exist in the same directory.
nodejs_binary(
    name = "express",
    data = [":app"],
    entry_point = ":app.js",
)
