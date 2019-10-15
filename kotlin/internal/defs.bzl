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
TOOLCHAIN_TYPE = "@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type"

# The name of the Kotlin compiler workspace.
KT_COMPILER_REPO = "com_github_jetbrains_kotlin"

KtJvmInfo = provider(
    fields = {
        "module_name": "the module name",
        "friend_paths": "The target(s) that this library can see the internals of.",
        "srcs": "the source files. [intelij-aspect]",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
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
