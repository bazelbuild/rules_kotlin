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
    "//kotlin/internal:defs.bzl",
    _JAVA_RUNTIME_TOOLCHAIN_TYPE = "JAVA_RUNTIME_TOOLCHAIN_TYPE",
    _JAVA_TOOLCHAIN_TYPE = "JAVA_TOOLCHAIN_TYPE",
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:plugins.bzl",
    _merge_plugin_infos = "merge_plugin_infos",
    _plugin_mappers = "mappers",
)
load(
    "//kotlin/internal:compiler_plugins.bzl",
    _plugins_to_classpaths = "plugins_to_classpaths",
    _plugins_to_options = "plugins_to_options",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)
load(
    "@bazel_tools//tools/jdk:toolchain_utils.bzl",
    "find_java_runtime_toolchain",
    "find_java_toolchain",
)

# INTERNAL ACTIONS #####################################################################################################
def _fold_jars_action(ctx, rule_kind, output_jar, input_jars, action_type = ""):
    """Set up an action to Fold the input jars into a normalized output jar."""
    args = ctx.actions.args()
    args.add_all([
        "--normalize",
        "--compression",
    ])
    args.add_all([
        "--deploy_manifest_lines",
        "Target-Label: %s" % str(ctx.label),
        "Injecting-Rule-Kind: %s" % rule_kind,
    ])
    args.add("--output", output_jar)
    args.add_all(input_jars, before_each = "--sources")
    ctx.actions.run(
        mnemonic = "KotlinFoldJars" + action_type,
        inputs = input_jars,
        outputs = [output_jar],
        executable = ctx.executable._singlejar,
        arguments = [args],
        progress_message = "Merging Kotlin output jar %s%s from %d inputs" % (
            ctx.label,
            "" if not action_type else " (%s)" % action_type,
            len(input_jars),
        ),
    )

_CONVENTIONAL_RESOURCE_PATHS = [
    "src/main/resources",
    "src/test/resources",
    "kotlin",
]

def _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix):
    if not path.startswith(resource_strip_prefix):
        fail("Resource file %s is not under the specified prefix to strip" % path)

    clean_path = path[len(resource_strip_prefix):]
    return resource_strip_prefix, clean_path

def _adjust_resources_path_by_default_prefixes(path):
    for cp in _CONVENTIONAL_RESOURCE_PATHS:
        dir_1, dir_2, rel_path = path.partition(cp)
        if rel_path:
            return dir_1 + dir_2, rel_path
    return "", path

def _adjust_resources_path(path, resource_strip_prefix):
    if resource_strip_prefix:
        return _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix)
    else:
        return _adjust_resources_path_by_default_prefixes(path)

def _resourcejar_args_action(ctx):
    res_cmd = []
    for f in ctx.files.resources:
        c_dir, res_path = _adjust_resources_path(f.short_path, ctx.attr.resource_strip_prefix)
        target_path = res_path
        if target_path[0] == "/":
            target_path = target_path[1:]
        line = "{target_path}={c_dir}{res_path}\n".format(
            res_path = res_path,
            target_path = target_path,
            c_dir = c_dir,
        )
        res_cmd.extend([line])
    zipper_args_file = ctx.actions.declare_file("%s_resources_zipper_args" % ctx.label.name)
    ctx.actions.write(zipper_args_file, "".join(res_cmd))
    return zipper_args_file

def _build_resourcejar_action(ctx):
    """sets up an action to build a resource jar for the target being compiled.
    Returns:
        The file resource jar file.
    """
    resources_jar_output = ctx.actions.declare_file(ctx.label.name + "-resources.jar")
    zipper_args = _resourcejar_args_action(ctx)
    ctx.actions.run_shell(
        mnemonic = "KotlinZipResourceJar",
        inputs = ctx.files.resources + [zipper_args],
        tools = [ctx.executable._zipper],
        outputs = [resources_jar_output],
        command = "{zipper} c {resources_jar_output} @{path}".format(
            path = zipper_args.path,
            resources_jar_output = resources_jar_output.path,
            zipper = ctx.executable._zipper.path,
        ),
        progress_message = "Creating intermediate resource jar %s" % ctx.label,
    )
    return resources_jar_output

def _partitioned_srcs(srcs):
    """Creates a struct of srcs sorted by extension. Fails if there are no sources."""
    kt_srcs = []
    java_srcs = []
    src_jars = []

    for f in srcs:
        if f.path.endswith(".kt"):
            kt_srcs.append(f)
        elif f.path.endswith(".java"):
            java_srcs.append(f)
        elif f.path.endswith(".srcjar"):
            src_jars.append(f)

    if not kt_srcs and not java_srcs and not src_jars:
        fail("no sources provided")

    return struct(
        kt = kt_srcs,
        java = java_srcs,
        all_srcs = kt_srcs + java_srcs,
        src_jars = src_jars,
    )

def _output_dir_path(ctx, aspect, dir_name):
    return "_kotlinc/%s_%s/%s_%s" % (ctx.label.name, aspect, ctx.label.name, dir_name)

def _compiler_directories(ctx):
    """Creates a dict of the necessary compiler directories for generating compile actions"""
    return dict(
        classdir = _output_dir_path(ctx, "jvm", "classes"),
        kotlin_generated_classdir = _output_dir_path(ctx, "jvm", "generated_classes"),
        sourcegendir = _output_dir_path(ctx, "jvm", "sourcegenfiles"),
        tempdir = _output_dir_path(ctx, "jvm", "temp"),
    )

def _compiler_toolchains(ctx):
    """Creates a struct of the relevant compilation toolchains"""
    return struct(
        kt = ctx.toolchains[_TOOLCHAIN_TYPE],
        java = find_java_toolchain(ctx, ctx.attr._java_toolchain),
        java_runtime = find_java_runtime_toolchain(ctx, ctx.attr._host_javabase),
    )

def _compiler_friends(ctx, friends):
    """Creates a struct of friends meta data"""

    # TODO extract and move this into common. Need to make it generic first.
    if len(friends) == 0:
        return struct(
            targets = [],
            module_name = _utils.derive_module_name(ctx),
            paths = [],
        )
    elif len(friends) == 1:
        if friends[0][_KtJvmInfo] == None:
            fail("only kotlin dependencies can be friends")
        elif ctx.attr.module_name:
            fail("if friends has been set then module_name cannot be provided")
        else:
            return struct(
                targets = friends,
                paths = friends[0][JavaInfo].compile_jars,
                module_name = friends[0][_KtJvmInfo].module_name,
            )
    else:
        fail("only one friend is possible")

def _compiler_deps(toolchains, friend, deps):
    """Encapsulates compiler dependency metadata."""
    dep_infos = [d[JavaInfo] for d in friend.targets + deps] + [toolchains.kt.jvm_stdlibs]
    return struct(
        deps = dep_infos,
        compile_jars = depset(
            transitive = [
                d.compile_jars
                for d in dep_infos
            ] + [
                d.transitive_compile_time_jars
                for d in dep_infos
            ],
        ),
    )

def _java_info_to_compile_jars(target):
    i = target[JavaInfo]
    if i == None:
        return None
    return i.compile_jars

# MAIN ACTIONS #########################################################################################################

def kt_jvm_compile_action(ctx, rule_kind, output_jar, compile_jar):
    """This macro sets up a compile action for a Kotlin jar.

    Args:
        rule_kind: The rule kind --e.g., `kt_jvm_library`.
        output_jar: The jar file that this macro will use as the output.
    Returns:
        A struct containing the providers JavaInfo (`java`) and `kt` (KtJvmInfo). This struct is not intended to be
        used as a legacy provider -- rather the caller should transform the result.
    """
    toolchains = _compiler_toolchains(ctx)
    dirs = _compiler_directories(ctx)
    srcs = _partitioned_srcs(ctx.files.srcs)
    friend = _compiler_friends(ctx, friends = getattr(ctx.attr, "friends", []))
    compile_deps = _compiler_deps(toolchains, friend, deps = ctx.attr.deps + ctx.attr.plugins)
    annotation_processors = _plugin_mappers.targets_to_annotation_processors(ctx.attr.plugins + ctx.attr.deps)
    transitive_runtime_jars = _plugin_mappers.targets_to_transitive_runtime_jars(ctx.attr.plugins + ctx.attr.deps)
    plugins = ctx.attr.plugins

    if toolchains.kt.experimental_use_abi_jars:
        kt_compile_jar = ctx.actions.declare_file(ctx.label.name + "-kt.abi.jar")
        _run_kt_builder_action(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            dirs = dirs,
            srcs = srcs,
            friend = friend,
            compile_deps = compile_deps,
            annotation_processors = annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            outputs = {
                "abi_jar": kt_compile_jar,
            },
            mnemonic = "KotlinCompileAbi",
        )
        if not srcs.java:
            compile_jar = kt_compile_jar
        else:
            java_compile_jar = ctx.actions.declare_file(ctx.label.name + "-java.abi.jar")
            java_info = java_common.compile(
                ctx,
                source_files = srcs.java,
                output = java_compile_jar,
                deps = compile_deps.deps + [JavaInfo(compile_jar = kt_compile_jar, output_jar = kt_compile_jar)],
                java_toolchain = toolchains.java,
                javac_opts = _javac_options_provider_to_flags(toolchains.kt.javac_options),
                host_javabase = toolchains.java_runtime,
            )
            compile_jar = ctx.actions.declare_file(ctx.label.name + ".abi.jar")
            _fold_jars_action(
                ctx,
                rule_kind = rule_kind,
                output_jar = compile_jar,
                action_type = "Abi",
                input_jars = [
                    kt_compile_jar,
                    java_common.run_ijar(
                        ctx.actions,
                        target_label = ctx.label,
                        jar = java_compile_jar,
                        java_toolchain = toolchains.java,
                    ),
                ],
            )

    _run_kt_builder_action(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        dirs = dirs,
        srcs = srcs,
        friend = friend,
        compile_deps = compile_deps,
        annotation_processors = annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
        outputs = {
            "output": output_jar,
            "kotlin_output_jdeps": ctx.outputs.jdeps,
            "kotlin_output_srcjar": ctx.outputs.srcjar,
        },
    )

    return struct(
        java = JavaInfo(
            output_jar = ctx.outputs.jar,
            compile_jar = compile_jar,
            source_jar = ctx.outputs.srcjar,
            #  jdeps = ctx.outputs.jdeps,
            deps = compile_deps.deps,
            runtime_deps = [d[JavaInfo] for d in ctx.attr.runtime_deps],
            exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
            neverlink = getattr(ctx.attr, "neverlink", False),
        ),
        kt = _KtJvmInfo(
            srcs = ctx.files.srcs,
            module_name = friend.module_name,
            language_version = toolchains.kt.api_version,
            # intellij aspect needs this.
            outputs = struct(
                jdeps = ctx.outputs.jdeps,
                jars = [struct(
                    class_jar = ctx.outputs.jar,
                    ijar = None,
                    source_jars = [ctx.outputs.srcjar],
                )],
            ),
        ),
    )

def _kotlinc_options_provider_to_flags(opts):
    if not opts:
        return ""
    flags = []
    if not opts.warn:
        flags.append("-nowarn")
    if opts.x_use_experimental:
        flags.append("-Xuse-experimental=kotlin.Experimental")
    return flags

def _javac_options_provider_to_flags(opts):
    if not opts:
        return ""
    flags = []
    if not opts.warn:
        flags.append("-nowarn")
    if opts.x_ep_disable_all_checks:
        flags.append("-XepDisableAllChecks")
    if opts.x_lint:
        flags.extend(["-Xlint:%s" % check for check in opts.x_lint])
    if opts.xd_suppress_notes:
        flags.append("-XDsuppressNotes")
    return flags

def _run_kt_builder_action(ctx, rule_kind, toolchains, dirs, srcs, friend, compile_deps, annotation_processors, transitive_runtime_jars, plugins, outputs, mnemonic = "KotlinCompile"):
    """Creates a KotlinBuilder action invocation."""
    args = _utils.init_args(ctx, rule_kind, friend.module_name)

    for f, path in dirs.items() + outputs.items():
        args.add("--" + f, path)

    args.add_all("--kotlin_passthrough_flags", _kotlinc_options_provider_to_flags(toolchains.kt.kotlinc_options))
    args.add_all("--javacopts", _javac_options_provider_to_flags(toolchains.kt.javac_options))

    args.add_all("--classpath", compile_deps.compile_jars)
    args.add_all("--sources", srcs.all_srcs, omit_if_empty = True)
    args.add_all("--source_jars", srcs.src_jars, omit_if_empty = True)

    args.add_joined("--kotlin_friend_paths", friend.paths, join_with = "\n")

    # Collect and prepare plugin descriptor for the worker.
    args.add_all(
        "--processors",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processor,
        omit_if_empty = True,
    )
    args.add_all(
        "--processorpath",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processorpath,
        omit_if_empty = True,
    )

    args.add_all(
        "--pluginpath",
        _plugins_to_classpaths(plugins),
        omit_if_empty = True,
    )
    args.add_all(
        "--plugin_options",
        _plugins_to_options(plugins),
        omit_if_empty = True,
    )

    progress_message = "%s %s { kt: %d, java: %d, srcjars: %d } for %s" % (
        mnemonic,
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars),
        ctx.var.get("TARGET_CPU", "UNKNOWN CPU"),
    )

    tools, input_manifests = ctx.resolve_tools(
        tools = [
            toolchains.kt.kotlinbuilder,
            toolchains.kt.kotlin_home,
        ],
    )

    ctx.actions.run(
        mnemonic = "KotlinCompile",
        inputs = depset(
            ctx.files.srcs,
            transitive = [compile_deps.compile_jars, transitive_runtime_jars],
        ),
        tools = tools,
        input_manifests = input_manifests,
        outputs = [f for f in outputs.values()],
        executable = toolchains.kt.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [args],
        progress_message = progress_message,
        env = {
            "LC_CTYPE": "en_US.UTF-8",  # For Java source files
        },
    )

def kt_jvm_produce_jar_actions(ctx, rule_kind):
    """Setup The actions to compile a jar and if any resources or resource_jars were provided to merge these in with the
    compilation output.

    Returns:
        see `kt_jvm_compile_action`.
    """

    # The jar that is compiled from sources.
    output_jar = ctx.outputs.jar

    # Indirection -- by default it is the same as the output_jar.
    kt_compile_output_jar = output_jar

    # A list of jars that should be merged with the output_jar, start with the resource jars if any were provided.
    output_merge_list = ctx.files.resource_jars

    # If this rule has any resources declared setup a zipper action to turn them into a jar and then add the declared
    # zipper output to the merge list.
    if len(ctx.files.resources) > 0:
        output_merge_list = output_merge_list + [_build_resourcejar_action(ctx)]

    # If the merge list is not empty the kotlin compiler should compile to an intermediate jar.
    if len(output_merge_list) > 0:
        # Declare the intermediate jar
        kt_compile_output_jar = ctx.actions.declare_file(ctx.label.name + "-ktclass.jar")

        # the first entry in the merge list is the result of the kotlin compile action.
        output_merge_list = [kt_compile_output_jar] + output_merge_list

        # Setup the merge action
        _fold_jars_action(ctx, rule_kind, output_jar, output_merge_list)

    # Setup the compile action.
    return kt_jvm_compile_action(
        ctx,
        rule_kind = rule_kind,
        output_jar = kt_compile_output_jar,
        compile_jar = output_jar,
    )
