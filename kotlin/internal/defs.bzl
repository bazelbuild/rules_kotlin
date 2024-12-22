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
    "//src/main/starlark/core/plugin:providers.bzl",
    _KspPluginInfo = "KspPluginInfo",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtCompilerPluginOption = "KtCompilerPluginOption",
    _KtPluginConfiguration = "KtPluginConfiguration",
)

# The Kotlin Toolchain type.
TOOLCHAIN_TYPE = "%s" % Label("//kotlin/internal:kt_toolchain_type")

# Java toolchains
JAVA_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:toolchain_type"
JAVA_RUNTIME_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:runtime_toolchain_type"

# The name of the Kotlin compiler workspace.
KT_COMPILER_REPO = "com_github_jetbrains_kotlin"

# The name of the KSP compiler plugin workspace
KSP_COMPILER_PLUGIN_REPO = "com_github_google_ksp"

KtJvmInfo = provider(
    fields = {
        "module_name": "the module name",
        "module_jars": "Jars comprising the module (logical compilation unit), a.k.a. associates",
        "exported_compiler_plugins": "compiler plugins to be invoked by targets depending on this.",
        "srcs": "the source files. [intelij-aspect]",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
        "language_version": "version of kotlin used. [intellij-aspect]",
        "transitive_compile_time_jars": "Returns the transitive set of Jars required to build the target. [intellij-aspect]",
        "transitive_runtime_jars": "Returns the transitive set of Jars required to run the target. [intellij-aspect]",
        "transitive_source_jars": "Returns the Jars containing source files of the current target and all of its transitive dependencies. [intellij-aspect]",
        "annotation_processing": "Generated annotation processing jars. [intellij-aspect]",
        "additional_generated_source_jars": "Returns additional Jars containing generated source files from kapt, ksp, etc. [bazel-bsp-aspect]",
        "all_output_jars": "Returns all the output Jars produced by this rule. [bazel-bsp-aspect]",
    },
)

KtCompilerPluginInfo = _KtCompilerPluginInfo

KspPluginInfo = _KspPluginInfo

KtCompilerPluginOption = _KtCompilerPluginOption

KtPluginConfiguration = _KtPluginConfiguration
