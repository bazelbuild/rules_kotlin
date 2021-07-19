# Copyright 2020 The Bazel Authors. All rights reserved.
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

# Separate starlark collection of purely rules for stardoc generation

load(
    "//kotlin/internal:toolchains.bzl",
    _define_kt_toolchain = "define_kt_toolchain",
    _kt_register_toolchains = "kt_register_toolchains",
)
load(
    "//kotlin/internal:opts.bzl",
    _kt_javac_options = "kt_javac_options",
    _kt_kotlinc_options = "kt_kotlinc_options",
)
load(
    ":jvm.bzl",
    _kt_compiler_plugin = "kt_compiler_plugin",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_import = "kt_jvm_import",
    _kt_jvm_library = "kt_jvm_library",
    _kt_jvm_test = "kt_jvm_test",
)
load(
    ":android.bzl",
    _kt_android_library = "kt_android_library",
    _kt_android_local_test = "kt_android_local_test",
)
load(
    ":js.bzl",
    _kt_js_import = "kt_js_import_macro",
    _kt_js_library = "kt_js_library_macro",
)
load(
    ":lint.bzl",
    _ktlint_config = "ktlint_config",
    _ktlint_fix = "ktlint_fix",
    _ktlint_test = "ktlint_test",
)

define_kt_toolchain = _define_kt_toolchain

kt_kotlinc_options = _kt_kotlinc_options

kt_javac_options = _kt_javac_options

kt_register_toolchains = _kt_register_toolchains
