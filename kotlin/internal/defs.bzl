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

# The Kotlin Toolchain type.
TOOLCHAIN_TYPE = "%s" % Label("//kotlin/internal:kt_toolchain_type")

# Java toolchains
JAVA_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:toolchain_type"
JAVA_RUNTIME_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:runtime_toolchain_type"

# The name of the Kotlin compiler workspace.
KT_COMPILER_REPO = "com_github_jetbrains_kotlin"

KtJvmInfo = provider(
    fields = {
        "module_name": "the module name",
        "exported_compiler_plugins": "compiler plugins to be invoked by targets depending on this.",
        "srcs": "the source files. [intelij-aspect]",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
        "language_version": "version of kotlin used. [intellij-aspect]",
    },
)

KtJsInfo = provider(
    fields = {
        "js": "The primary output of the library",
        "js_map": "The map file for the library",
        "jar": "A jar of the library.",
        "srcjar": "The jar containing the sources of the library",
    },
)

KtCompilerPluginInfo = provider(
    fields = {
        "plugin_jars": "List of plugin jars.",
        "classpath": "The kotlin compiler plugin classpath.",
        "stubs": "Run this plugin during kapt stub generation.",
        "compile": "Run this plugin during koltinc compilation.",
        "options": "List of plugin options, represented as structs with an id and a value field, to be passed to the compiler",
    },
)
