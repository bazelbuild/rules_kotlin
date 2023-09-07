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
# limitations under the License.#
load(
    "//src/main/starlark/core:providers.bzl",
    _KspPluginInfo = "KspPluginInfo",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtJsInfo = "KtJsInfo",
    _KtJvmInfo = "KtJvmInfo",
)
load("//src/main/starlark/core/repositories:versions.bzl", "constants")

# The Kotlin Toolchain type.
TOOLCHAIN_TYPE = "%s" % Label("//kotlin/internal:kt_toolchain_type")

# Java toolchains
JAVA_TOOLCHAIN_TYPE = constants.JAVA_TOOLCHAIN_TYPE
JAVA_RUNTIME_TOOLCHAIN_TYPE = constants.JAVA_RUNTIME_TOOLCHAIN_TYPE

# Upstream provider for Java plugins
JavaPluginInfo = getattr(java_common, "JavaPluginInfo")

# The name of the Kotlin compiler workspace.
KT_COMPILER_REPO = constants.KT_COMPILER_REPO

# The name of the KSP compiler plugin workspace
KSP_COMPILER_PLUGIN_REPO = constants.KSP_COMPILER_PLUGIN_REPO

# Providers from the current kotlin repository.
KtJvmInfo = _KtJvmInfo
KtJsInfo = _KtJsInfo
KtCompilerPluginInfo = _KtCompilerPluginInfo
KspPluginInfo = _KspPluginInfo
