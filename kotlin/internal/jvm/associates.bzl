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

def _get_associates(ctx):
    """Creates a struct of associates meta data"""

    friends_legacy = getattr(ctx.attr, "friends", [])
    associates = getattr(ctx.attr, "associates", [])

    if friends_legacy:
        print("WARNING: friends=[...] is deprecated, please prefer associates=[...] instead.")
        if associates:
            fail("friends= may not be used together with associates=. Use one or the other.")
        elif ctx.attr.testonly == False:
            fail("Only testonly targets can use the friends attribute. ")
        else:
            associates = friends_legacy

    if not bool(associates):
        return struct(
            targets = [],
            module_name = _utils.derive_module_name(ctx),
            jars = [],
        )
    elif ctx.attr.module_name:
        fail("if associates have been set then module_name cannot be provided")
    else:
        jars = [depset([a], transitive = a[_KtJvmInfo].module_jars) for a in associates]
        module_names = _sets.copy_of([x[_KtJvmInfo].module_name for x in associates])
        if len(module_names) > 1:
            fail("Dependencies from several different kotlin modules cannot be associated. " +
                 "Associates can see each other's \"internal\" members, and so must only be " +
                 "used with other targets in the same module: \n%s" % module_names)
        if len(module_names) < 1:
            # This should be impossible
            fail("Error in rules - a KtJvmInfo was found which did not have a module_name")
        return struct(
            targets = associates,
            jars = jars,
            module_name = list(module_names)[0],
        )

def _flatten_jars(nested_jars_depset):
    """Returns a list of strings containing the compile_jars for depset of targets.

    This ends up unwinding the nesting of depsets, since compile_jars contains depsets inside
    the nested_jars targets, which themselves are depsets.  This function is intended to be called
    lazily form within Args.add_all(map_each) as it collapses depsets.
    """
    compile_jars_depsets = [
        target[JavaInfo].compile_jars
        for target in nested_jars_depset.to_list()
        if target[JavaInfo].compile_jars
    ]
    return [file.path for file in depset(transitive = compile_jars_depsets).to_list()]

associate_utils = struct(
    get_associates = _get_associates,
    flatten_jars = _flatten_jars,
)
