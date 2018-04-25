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
########################################################################################################################
# Providers
########################################################################################################################
_defs = struct(
    DEFAULT_TOOLCHAIN = "@io_bazel_rules_kotlin//kotlin:default_toolchain",
    # The name of the Kotlin compiler workspace.
    KT_COMPILER_REPO = "com_github_jetbrains_kotlin",
    # The name of the rules repo. Centralised so it's easy to change.
    REPO_ROOT = "io_bazel_rules_kotlin",
    TOOLCHAIN_TYPE = "@io_bazel_rules_kotlin//kotlin:kt_toolchain_type",
)

_KtInfo = provider(
    fields = {
        "srcs": "the source files. [intelij-aspect]",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
    },
)

_KtPluginInfo = provider(
    fields = {
        "processors": "a serializeable list of structs containing an annotation processor definitions",
    },
)

kt = struct(
    defs = _defs,
    info = struct(
        KtInfo = _KtInfo,
        KtPluginInfo = _KtPluginInfo,
    ),
)
