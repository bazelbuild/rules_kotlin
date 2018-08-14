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
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:plugins.bzl",
    _merge_plugin_infos = "merge_plugin_infos"
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

# INTERNAL ACTIONS #####################################################################################################
def _fold_jars_action(ctx, rule_kind, output_jar, input_jars):
    args = [
        "--normalize",
        "--compression",
        "--deploy_manifest_lines",
        "Target-Label: %s" % str(ctx.label),
        "Injecting-Rule-Kind: %s" % rule_kind,
        "--output",
        output_jar.path,
    ]
    for i in input_jars:
        args += ["--sources", i.path]
    ctx.action(
        mnemonic = "KotlinFoldOutput",
        inputs = input_jars,
        outputs = [output_jar],
        executable = ctx.executable._singlejar,
        arguments = args,
        progress_message = "Merging Kotlin output jar " + output_jar.short_path,
    )

_CONVENTIONAL_RESOURCE_PATHS = [
    "src/main/resources",
    "src/test/resources",
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

def _add_resources_cmd(ctx):
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
    return "".join(res_cmd)

def _build_resourcejar_action(ctx):
    resources = _add_resources_cmd(ctx)
    resources_jar_output = ctx.actions.declare_file(ctx.label.name + "-resources.jar")
    zipper_arg_path = ctx.actions.declare_file("%s_resources_zipper_args" % ctx.label.name)
    ctx.file_action(zipper_arg_path, resources)
    cmd = """
rm -f {resources_jar_output}
{zipper} c {resources_jar_output} @{path}
""".format(
        path = zipper_arg_path.path,
        resources_jar_output = resources_jar_output.path,
        zipper = ctx.executable._zipper.path,
    )
    ctx.action(
        mnemonic = "KotlinZipResourceJar",
        inputs = ctx.files.resources + [ctx.executable._zipper, zipper_arg_path],
        outputs = [resources_jar_output],
        command = cmd,
        progress_message = "Creating intermediate resource jar %s" % ctx.label,
        arguments = [],
    )
    return resources_jar_output

# MAIN ACTIONS #########################################################################################################
def _declare_output_directory(ctx, aspect, dir_name):
    return ctx.actions.declare_directory("_kotlinc/%s_%s/%s_%s" % (ctx.label.name, aspect, ctx.label.name, dir_name))

def _partition_srcs(srcs):
    """Partition sources for the jvm aspect."""
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

    kt = depset(kt_srcs)
    java = depset(java_srcs)

    return struct(
        kt = kt,
        java = java,
        all_srcs = kt + java,
        src_jars = depset(src_jars),
    )

def kt_jvm_compile_action(ctx, rule_kind, output_jar):
    """This macro sets up a compile action for a Kotlin jar.

    Args:
        rule_kind: The rule kind --e.g., `kt_jvm_library`.
        output_jar: The jar file that this macro will use as the output of the action.
    Returns:
        A struct containing the providers JavaInfo (`java`) and `kt` (KtJvmInfo). This struct is not intended to be
        used as a legacy provider -- rather the caller should transform the result.
    """
    toolchain = ctx.toolchains[_TOOLCHAIN_TYPE]

    srcs = _partition_srcs(ctx.files.srcs)
    if (len(srcs.kt) + len(srcs.java) == 0) and len(srcs.src_jars) == 0:
        fail("no sources provided")

    # TODO extract and move this into common. Need to make it generic first.
    friends = getattr(ctx.attr, "friends", [])
    deps = [d[JavaInfo] for d in friends + ctx.attr.deps] + [toolchain.jvm_stdlibs]
    compile_jars = java_common.merge(deps).compile_jars

    if len(friends) == 0:
        module_name = _utils.derive_module_name(ctx)
        friend_paths = depset()
    elif len(friends) == 1:
        if friends[0][_KtJvmInfo] == None:
            fail("only kotlin dependencies can be friends")
        elif ctx.attr.module_name:
            fail("if friends has been set then module_name cannot be provided")
        else:
            friend_paths = depset([j.path for j in friends[0][JavaInfo].compile_jars])
            module_name = friends[0][_KtJvmInfo].module_name
    else:
        fail("only one friend is possible")

    classes_directory = _declare_output_directory(ctx, "jvm", "classes")
    generated_classes_directory = _declare_output_directory(ctx, "jvm", "generated_classes")
    sourcegen_directory = _declare_output_directory(ctx, "jvm", "sourcegenfiles")
    temp_directory = _declare_output_directory(ctx, "jvm", "temp")

    args = _utils.init_args(ctx, rule_kind, module_name)

    args.add("--classdir", classes_directory)
    args.add("--sourcegendir", sourcegen_directory)
    args.add("--tempdir", temp_directory)
    args.add("--kotlin_generated_classdir", generated_classes_directory)

    args.add("--output", output_jar)
    args.add("--kotlin_output_jdeps", ctx.outputs.jdeps)
    args.add("--kotlin_output_srcjar", ctx.outputs.srcjar)

    args.add("--kotlin_friend_paths", "\n".join(friend_paths.to_list()))

    args.add("--classpath", compile_jars)
    args.add_all("--sources", srcs.all_srcs, omit_if_empty = True)
    args.add_all("--source_jars", srcs.src_jars, omit_if_empty = True)

    # Collect and prepare plugin descriptor for the worker.
    plugin_info = _merge_plugin_infos(ctx.attr.plugins + ctx.attr.deps)
    if len(plugin_info.annotation_processors) > 0:
        args.add("--kotlin_plugins", plugin_info.to_json())

    progress_message = "Compiling Kotlin to JVM %s { kt: %d, java: %d, srcjars: %d }" % (
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars),
    )

    inputs, _, input_manifests = ctx.resolve_command(tools = [toolchain.kotlinbuilder, toolchain.kotlin_home])
    ctx.actions.run(
        mnemonic = "KotlinCompile",
        inputs = depset(inputs) + ctx.files.srcs + compile_jars,
        outputs = [
            output_jar,
            ctx.outputs.jdeps,
            ctx.outputs.srcjar,
            sourcegen_directory,
            classes_directory,
            temp_directory,
            generated_classes_directory,
        ],
        executable = toolchain.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [args],
        progress_message = progress_message,
        input_manifests = input_manifests,
    )

    return struct(
        java = JavaInfo(
            output_jar = ctx.outputs.jar,
            compile_jar = ctx.outputs.jar,
            source_jar = ctx.outputs.srcjar,
            #  jdeps = ctx.outputs.jdeps,
            deps = deps,
            runtime_deps = [d[JavaInfo] for d in ctx.attr.runtime_deps],
            exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
            neverlink = getattr(ctx.attr, "neverlink", False),
        ),
        kt = _KtJvmInfo(
            srcs = ctx.files.srcs,
            module_name = module_name,
            # intelij aspect needs this.
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

def kt_jvm_produce_jar_actions(ctx, rule_kind):
    """Setup The actions to compile a jar and merge additional resources onto it. see `kt_jvm_compile_action`."""

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
        kt_compile_output_jar = ctx.new_file(ctx.label.name + "-ktclass.jar")

        # the first entry in the merge list is the result of the kotlin compile action.
        output_merge_list = [kt_compile_output_jar] + output_merge_list

        # Setup the merge action
        _fold_jars_action(ctx, rule_kind, output_jar, output_merge_list)

    # Setup the compile action.
    return kt_jvm_compile_action(
        ctx,
        rule_kind = rule_kind,
        output_jar = kt_compile_output_jar,
    )
