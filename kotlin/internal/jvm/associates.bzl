# Copyright 2020 The Bazel Authors. All rights reserved.
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
load("@rules_java//java:defs.bzl", "JavaInfo")
load(
    "//kotlin/internal:defs.bzl",
    _KtJvmInfo = "KtJvmInfo",
)
load(
    "//kotlin/internal/utils:sets.bzl",
    _sets = "sets",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

def _collect_associates(ctx, toolchains, associate):
    """Collects the associate jars from the provided dependency and returns
    them as a depset.

    There are two outcomes for this marco:
    1. When `experimental_strict_associate_dependencies` is enabled and the tag override has not been provided, only the
        direct java_output class jars will be collected for each associate target, especially useful when using
        `experimental_treat_internal_as_private_in_abi_jars` and `experimental_remove_private_classes_in_abi_jars`.
    2. When `experimental_strict_associate_dependencies` is disabled, the complete transitive set of compile jars will
        be collected for each assoicate target.
    """
    jars_depset = None
    if (toolchains.kt.experimental_strict_associate_dependencies and
        "kt_experimental_strict_associate_dependencies_incompatible" not in ctx.attr.tags):
        jars_depset = depset(direct = [a.class_jar for a in associate[JavaInfo].java_outputs])
    else:
        # need to exclude the associate compile jar but include its class_jar
        ass_class_jars = associate[JavaInfo].compile_jars.to_list()
        for cj in [a.compile_jar for a in associate[JavaInfo].java_outputs]:
            ass_class_jars.remove(cj)
        jars_depset = depset(direct = [a.class_jar for a in associate[JavaInfo].java_outputs], transitive = ass_class_jars)
    return jars_depset

def _java_info(target):
    return target[JavaInfo] if JavaInfo in target else None

def _get_associates(ctx, toolchains, associates):
    """Creates a struct of associates meta data"""
    if not associates:
        return struct(
            module_name = _utils.derive_module_name(ctx),
            jars = depset(),
            dep_infos = [],
        )
    elif ctx.attr.module_name:
        fail("If associates have been set then module_name cannot be provided")
    else:
        jars = []
        module_names = []
        java_infos = []
        for a in associates:
            jars.append(_collect_associates(ctx = ctx, toolchains = toolchains, associate = a))
            module_names.append(a[_KtJvmInfo].module_name)
            java_infos.append(_java_info(a))
        module_names = list(_sets.copy_of(module_names))

        if len(module_names) > 1:
            fail("Dependencies from several different kotlin modules cannot be associated. " +
                 "Associates can see each other's \"internal\" members, and so must only be " +
                 "used with other targets in the same module: \n%s" % module_names)
        if len(module_names) < 1:
            # This should be impossible
            fail("Error in rules - a KtJvmInfo was found which did not have a module_name")
        return struct(
            jars = depset(transitive = jars),
            module_name = module_names[0],
            dep_infos = java_infos,
        )

associate_utils = struct(
    get_associates = _get_associates,
)
