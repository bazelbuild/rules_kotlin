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
package(default_visibility = ["//visibility:public"])

load("//kotlin:toolchains.bzl", "define_kt_toolchain")
load("//kotlin/internal:bootstrap.bzl", "kt_toolchain_ide_info")

config_setting(
    name = "builder_debug_setting",
    values = {
        "define": "kt_debug=1"
    }
)

toolchain_type(
    name = "kt_toolchain_type",
    visibility = ["//visibility:public"]
)

define_kt_toolchain(
    name = "default_toolchain",
    debug=select({
        "//kotlin:builder_debug_setting": 1,
        "//conditions:default": 0
    })
)

kt_toolchain_ide_info(name="kt_toolchain_ide_info")

exports_files(["toolchains.bzl", "kotlin.bzl"], visibility=["//docs:__subpackages__"])
