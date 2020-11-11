# Copyright 2017 The Bazel Authors. All rights reserved.
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

load(
    "//kotlin/internal/repositories:repositories.bzl",
    _kotlin_repositories = "kotlin_repositories",
)
load(
    "//kotlin:rules.bzl",
    _define_kt_toolchain = "define_kt_toolchain",
    _kt_kotlinc_options = "kt_kotlinc_options",
    _kt_javac_options = "kt_javac_options",
    _kt_android_library = "kt_android_library",
    _kt_compiler_plugin = "kt_compiler_plugin",
    _kt_js_import = "kt_js_import",
    _kt_js_library = "kt_js_library",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_import = "kt_jvm_import",
    _kt_jvm_library = "kt_jvm_library",
    _kt_jvm_test = "kt_jvm_test",
    _kt_register_toolchains = "kt_register_toolchains",
)

kotlin_repositories = _kotlin_repositories
define_kt_toolchain = _define_kt_toolchain
kt_kotlinc_options = _kt_kotlinc_options
kt_javac_options = _kt_javac_options
kt_js_library = _kt_js_library
kt_js_import = _kt_js_import
kt_register_toolchains = _kt_register_toolchains
kt_jvm_binary = _kt_jvm_binary
kt_jvm_import = _kt_jvm_import
kt_jvm_library = _kt_jvm_library
kt_jvm_test = _kt_jvm_test
kt_android_library = _kt_android_library
kt_compiler_plugin = _kt_compiler_plugin
