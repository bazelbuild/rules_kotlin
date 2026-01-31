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
load("@bazel_skylib//lib:sets.bzl", _sets = "sets")
load(
    "@rules_java//java:defs.bzl",
    "JavaInfo",
)
load("//kotlin/internal/jvm:associates.bzl", _associate_utils = "associate_utils")

def _java_info(target):
    return target[JavaInfo] if JavaInfo in target else None

def _create_pruned_java_infos(java_info):
    """Creates JavaInfo objects that only expose direct compile jars, not transitive.

    Returns a list of JavaInfo objects, one per compile jar.
    """
    if java_info == None:
        return []
    # Create a new JavaInfo for each compile jar
    # This prevents java_common.compile from seeing transitive deps
    compile_jars_list = java_info.compile_jars.to_list()
    if not compile_jars_list:
        return []
    result = []
    for jar in compile_jars_list:
        result.append(JavaInfo(
            output_jar = jar,
            compile_jar = jar,
            deps = [],  # No transitive deps
            neverlink = True,  # Don't contribute to runtime classpath
        ))
    return result

def _jvm_deps(ctx, toolchains, associate_deps, deps = [], deps_java_infos = [], exports = [], runtime_deps = []):
    """Encapsulates jvm dependency metadata."""
    associates = _associate_utils.get_associates(
        ctx,
        toolchains = toolchains,
        associates = associate_deps,
    )
    dep_infos = (
        deps_java_infos +
        [_java_info(d) for d in deps] +
        associates.dep_infos +
        [toolchains.kt.jvm_stdlibs]
    )

    prune_transitive_deps = (toolchains.kt.experimental_prune_transitive_deps and
        not "kt_experimental_prune_transitive_deps_incompatible" in ctx.attr.tags)

    # Reduced classpath, exclude transitive deps from compilation
    if prune_transitive_deps:
        transitive = [
            d.compile_jars
            for d in dep_infos
        ]
    else:
        transitive = [
            d.compile_jars
            for d in dep_infos
        ] + [
            d.transitive_compile_time_jars
            for d in dep_infos
        ]

    compile_depset_list = depset(transitive = transitive + [associates.jars]).to_list()
    compile_depset_list_filtered = [jar for jar in compile_depset_list if not _sets.contains(associates.abi_jar_set, jar)]

    # Create pruned deps for Java compilation when prune_transitive_deps is enabled
    # This is needed because java_common.compile() uses transitive_compile_time_jars from JavaInfo deps
    pruned_deps_for_java = None
    if prune_transitive_deps:
        pruned_deps_for_java = []
        for d in dep_infos:
            pruned_deps_for_java.extend(_create_pruned_java_infos(d))

    return struct(
        module_name = associates.module_name,
        deps = dep_infos,
        pruned_deps_for_java = pruned_deps_for_java,
        exports = [_java_info(d) for d in exports],
        associate_jars = associates.jars,
        compile_jars = depset(direct = compile_depset_list_filtered),
        runtime_deps = [_java_info(d) for d in runtime_deps],
    )

jvm_deps_utils = struct(
    jvm_deps = _jvm_deps,
)
