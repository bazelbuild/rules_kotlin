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
    ":jvm.bzl",
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
    _kt_js_import = "kt_js_import",
    _kt_js_library = "kt_js_library",
)
load(
    ":lint.bzl",
    _ktlint_config = "ktlint_config",
    _ktlint_fix = "ktlint_fix",
    _ktlint_test = "ktlint_test",
)
load(
    ":core.bzl",
    _define_kt_toolchain = "define_kt_toolchain",
    _kt_compiler_plugin = "kt_compiler_plugin",
    _kt_register_toolchains = "kt_register_toolchains",
)

define_kt_toolchain = _define_kt_toolchain
kt_register_toolchains = _kt_register_toolchains
kt_compiler_plugin = _kt_compiler_plugin

# buildifier: disable=unused-variable Will be removed in a future release
def kt_kotlinc_options(**kwargs):
    fail("use load('//kotlin:core.bzl', kt_kotlinc_options)")

# buildifier: disable=unused-variable Will be removed in a future release
def kt_javac_options(**kwargs):
    fail("use load('//kotlin:core.bzl', kt_javac_options)")

def kt_js_library(**kwargs):
    """
    Forwarding macro for kt_js_library

    Deprecated:
        kt_js_library should be loaded from //kotlin:js.bzl
    """
    print("kt_js_library should be loaded from //kotlin:js.bzl")
    _kt_js_library(**kwargs)

def kt_js_import(**kwargs):
    """
     Forwarding macro for kt_js_import

     Deprecated:
        kt_js_import should be loaded from //kotlin:js.bzl
    """
    print("kt_js_import should be loaded from //kotlin:js.bzl")
    _kt_js_import(**kwargs)

def kt_jvm_binary(**kwargs):
    """
    Forwarding macro for kt_jvm_binary

    Deprecated:
        kt_jvm_binary should be loaded from //kotlin:jvm.bzl
    """
    print("kt_jvm_binary should be loaded from //kotlin:jvm.bzl")
    _kt_jvm_binary(**kwargs)

def kt_jvm_import(**kwargs):
    """
    Forwarding macro for kt_jvm_import

    Deprecated:
        kt_jvm_import should be loaded from //kotlin:jvm.bzl
    """
    print("kt_jvm_import should be loaded from //kotlin:jvm.bzl")
    _kt_jvm_import(**kwargs)

def kt_jvm_library(**kwargs):
    """
    Forwarding macro for kt_jvm_library

    Deprecated:
        kt_jvm_library should be loaded from //kotlin:jvm.bzl
    """
    print("kt_jvm_library should be loaded from //kotlin:jvm.bzl")
    _kt_jvm_library(**kwargs)

def kt_jvm_test(**kwargs):
    """
    Forwarding macro for kt_jvm_test

    Deprecated:
        kt_jvm_test should be loaded from //kotlin:jvm.bzl
    """
    print("kt_jvm_test should be loaded from //kotlin:jvm.bzl")
    _kt_jvm_test(**kwargs)

def kt_android_library(**kwargs):
    """
    Forwarding macro for kt_android_local_test

    Deprecated:
        kt_android_library should be loaded from //kotlin:android.bzl
    """
    print("kt_android_library should be loaded from //kotlin:android.bzl")
    _kt_android_library(**kwargs)

def kt_android_local_test(**kwargs):
    """
    Forwarding macro for kt_android_local_test

    Deprecated:
        kt_android_local_test should be loaded from //kotlin:android.bzl
    """
    print("kt_android_local_test should be loaded from //kotlin:android.bzl")
    _kt_android_local_test(**kwargs)

def ktlint_config(**kwargs):
    """
    Forwarding macro for ktlint_config

    Deprecated:
        ktlint_config should be loaded from //kotlin:lint.bzl
    """
    print("ktlint_config should be loaded from //kotlin:lint.bzl")
    _ktlint_config(**kwargs)

def ktlint_fix(**kwargs):
    """
    Forwarding macro for ktlint_fix

    Deprecated:
        ktlint_fix should be loaded from //kotlin:lint.bzl
    """
    print("ktlint_fix should be loaded from //kotlin:lint.bzl")
    _ktlint_fix(**kwargs)

def ktlint_test(**kwargs):
    """
    Forwarding macro for ktlint_test

    Deprecated:
        ktlint_test should be loaded from //kotlin:lint.bzl
    """
    print("ktlint_test should be loaded from //kotlin:lint.bzl")
    _ktlint_test(**kwargs)
