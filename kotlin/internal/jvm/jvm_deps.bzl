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
load(
    "@rules_java//java:defs.bzl",
    "JavaInfo",
)
load("//kotlin/internal/jvm:associates.bzl", _associate_utils = "associate_utils")

def _jvm_deps(ctx, toolchains, associate_deps, deps = [], additional_deps = [], exports = [], runtime_deps = []):
    """Encapsulates jvm dependency metadata."""
    associates = _associate_utils.get_associates(
        ctx,
        toolchains = toolchains,
        associates = associate_deps,
    )

    # TODO: cleaup this API, probably remove it
    return struct(
        module_name = associates.module_name,
        deps = deps,
        additional_deps = additional_deps,
        exports = exports,
        runtime_deps = runtime_deps,
        associates = associates,
    )

jvm_deps_utils = struct(
    jvm_deps = _jvm_deps,
)
