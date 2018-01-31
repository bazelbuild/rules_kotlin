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
package(default_visibility = ["//visibility:private"])
load("@io_bazel_skydoc//skylark:skylark.bzl", "skylark_doc", "skylark_library" )

skylark_library(
    name = "doc_public",
    srcs = [
        "kotlin.bzl"
    ],
    visibility=["//visibility:private"]
)

skylark_doc(
    name = "docs",
    deps = [
        ":doc_public",
    ],
    format = "html",
    site_root = select({
        ":local": "/tmp/rules_kotlin",
        "//conditions:default": "https://hsyed.github.io/rules_kotlin"
#        "//conditions:default": "https://bazelbuild.github.io/rules_kotlin"
    }),
    visibility = ["//visibility:public"]
)

config_setting(
    name = "local",
    values = {
        "define": "local=1",
    }
)