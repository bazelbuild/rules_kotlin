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

def _get_associates(ctx, associates):
    """Creates a struct of associates meta data"""
    if not associates:
        return struct(
            targets = [],
            module_name = _utils.derive_module_name(ctx),
            jars = depset(),
        )
    elif ctx.attr.module_name:
        fail("If associates have been set then module_name cannot be provided")
    else:
        jars = []
        module_names = []
        for a in associates:
            jars.append(depset([a.class_jar for a in a[JavaInfo].java_outputs], transitive = [a[_KtJvmInfo].module_jars]))
            module_names.append(a[_KtJvmInfo].module_name)
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
        )

associate_utils = struct(
    get_associates = _get_associates,
)
