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
"""Kotlin Toolchains

This file contains macros for defining and registering specific toolchains.

### Examples

To override a tool chain use the appropriate macro in a `BUILD` file to declare the toolchain:

```bzl
load("@io_bazel_rules_kotlin//kotlin:toolchains.bzl", "define_kt_jvm_toolchain")

define_kt_jvm_toolchain(
    name= "custom_toolchain",
    api_version = "1.1",
    language_version = "1.1",
)
```
and then register it in the `WORKSPACE`:
```bzl
register_toolchains("//:custom_toolchain")
```
"""
# The toolchain rules are not made private, at least the jvm ones so that they may be introspected in Intelij.
_common_attrs = {
    'language_version': attr.string(default="1.2", values=["1.1", "1.2"]),
    'api_version': attr.string(default="1.2", values=["1.1", "1.2"]),
    "coroutines": attr.string(default="enable", values=["enable", "warn", "error"]),
}

_kt_jvm_attrs = dict(_common_attrs.items() + {
    'jvm_target': attr.string(default="1.8", values=["1.6", "1.8"]),
}.items())

def _kotlin_jvm_toolchain_impl(ctx):
    toolchain = platform_common.ToolchainInfo(
        language_version = ctx.attr.language_version,
        api_version = ctx.attr.api_version,
        jvm_target = ctx.attr.jvm_target,
        coroutines = ctx.attr.coroutines
    )
    return [toolchain]

kt_jvm_toolchain = rule(
    implementation = _kotlin_jvm_toolchain_impl,
    attrs = _kt_jvm_attrs
)
"""The kotlin jvm toolchain
Args:
  language_version: the -languag_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html).
  api_version: the -api_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html).
  jvm_target: the -jvm_target flag.
  coroutines: the -Xcoroutines flag, enabled by default as it's considered production ready 1.2.0 onward.
"""

def define_kt_jvm_toolchain(name, language_version=None, api_version=None, jvm_target=None, coroutines=None):
    """Define a Kotlin JVM Toolchain, the name is used in the `toolchain` rule so can be used to register the toolchain in the WORKSPACE file."""
    impl_name = name + "_impl"
    kt_jvm_toolchain(
        name = impl_name,
        language_version = language_version,
        api_version = api_version,
        jvm_target = jvm_target,
        coroutines = coroutines,
        visibility = ["//visibility:public"]
    )
    native.toolchain(
        name = name,
        toolchain_type = "@io_bazel_rules_kotlin//kotlin:kt_jvm_toolchain_type",
        toolchain = impl_name,
        visibility = ["//visibility:public"]
    )

def kt_register_jvm_toolchain():
    """Register the default JVM toolchain."""
    native.register_toolchains("@io_bazel_rules_kotlin//kotlin:default_jvm_toolchain")
