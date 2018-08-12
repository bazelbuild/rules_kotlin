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
load("//kotlin/internal:defs.bzl", "KtJvmInfo", "TOOLCHAIN_TYPE")
load("//kotlin/internal/jvm:plugins.bzl", "plugins")
load("//kotlin/internal/common:common.bzl", "common")

# MISC UTILS ###########################################################################################################
def _partition_srcs(srcs):
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

# JAR ACTIONS ##########################################################################################################
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

def kt_jvm_compile_action(ctx, rule_kind, output_jar, srcs):
    """This macro performs a compile operation in a single action.

    Args:
      rule_kind: The rule kind,
      output_jar: The jar file that this macro will use as the output of the action.
      module_name: The Kotlin module name, this must be provided and is used by the compiler for symbol mangling in
         advanced use cases.
      compile_jars: The compile time jars provided on the classpath for the compile operations -- callers are
        responsible for preparing the classpath. The stdlib (and jdk7 + jdk8) should generally be added to the classpath
        by the caller -- kotlin-reflect could be optional.
      friend_paths: A list of jars paths that this compilation unit should have package private access to.
      srcs: a struct with the various input sources partitioned.
    """
    toolchain = ctx.toolchains[TOOLCHAIN_TYPE]

    friends = getattr(ctx.attr, "friends", [])
    deps = [d[JavaInfo] for d in friends + ctx.attr.deps] + [toolchain.jvm_stdlibs]
    compile_jars = java_common.merge(deps).compile_jars

    if len(friends) == 0:
        module_name = common.derive_module_name(ctx)
        friend_paths = depset()
    elif len(friends) == 1:
        if friends[0][KtJvmInfo] == None:
            fail("only kotlin dependencies can be friends")
        elif ctx.attr.module_name:
            fail("if friends has been set then module_name cannot be provided")
        else:
            friend_paths = depset([j.path for j in friends[0][JavaInfo].compile_jars])
            module_name = friends[0][KtJvmInfo].module_name
    else:
        fail("only one friend is possible")

    classes_directory = common.declare_output_directory(ctx, "jvm", "classes")
    generated_classes_directory = common.declare_output_directory(ctx, "jvm", "generated_classes")
    sourcegen_directory = common.declare_output_directory(ctx, "jvm", "sourcegenfiles")
    temp_directory = common.declare_output_directory(ctx, "jvm", "temp")

    args = common.init_args(ctx, rule_kind, module_name)

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
    plugin_info = plugins.merge_plugin_infos(ctx.attr.plugins + ctx.attr.deps)
    if len(plugin_info.annotation_processors) > 0:
        args.add("--kotlin_plugins", plugin_info.to_json())

    progress_message = "Compiling Kotlin to JVM %s { kt: %d, java: %d, srcjars: %d }" % (
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars),
    )

    inputs, _, input_manifests = ctx.resolve_command(tools = [toolchain.kotlinbuilder])
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

    # create the java provider and the kotlin provider. Whilst a struct is being returned, and this is a valid way of
    # creating a provider, it is intended that the client transforms this into an form.
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
        kt = KtJvmInfo(
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

def kt_jvm_produce_jar_actions(ctx, rule_kind, src_jars = []):
    """Setup a kotlin compile action. This method takes care of all of the aspects of producing a jar.

    Specifically this action will conditionally set up actions to fold resources and resourcejars and merge them onto a
    jar compiled by the builder. It indirects the output_jar -- i.e., if no resources or resource jars are present it
    won't do anything.

    Args:
        ctx: The rule context.
    Returns:
        A JavaInfo struct for the output jar that this macro will build.
    """

    # The main output jars
    output_jar = ctx.outputs.jar

    # The output of the compile step may be combined (folded) with other entities -- e.g., other class files from annotation processing, embedded resources.
    kt_compile_output_jar = output_jar

    # the list of jars to merge into the final output, start with the resource jars if any were provided.
    output_merge_list = ctx.files.resource_jars

    # If this rule has any resources declared setup a zipper action to turn them into a jar and then add the declared zipper output to the merge list.
    if len(ctx.files.resources) > 0:
        output_merge_list = output_merge_list + [_build_resourcejar_action(ctx)]

    # If this compile operation requires merging other jars setup the compile operation to go to a intermediate file and add that file to the merge list.
    if len(output_merge_list) > 0:
        # Intermediate jar containing the Kotlin compile output.
        kt_compile_output_jar = ctx.new_file(ctx.label.name + "-ktclass.jar")

        # If we setup indirection than the first entry in the merge list is the result of the kotlin compile action.
        output_merge_list = [kt_compile_output_jar] + output_merge_list

    srcs = _partition_srcs(ctx.files.srcs)

    if (len(srcs.kt) + len(srcs.java) == 0) and len(srcs.src_jars) == 0:
        fail("no sources provided")

    # setup the merge action if needed.
    if len(output_merge_list) > 0:
        _fold_jars_action(ctx, rule_kind, output_jar, output_merge_list)

    # setup the compile action.
    return kt_jvm_compile_action(
        ctx,
        rule_kind = rule_kind,
        output_jar = kt_compile_output_jar,
        srcs = srcs,
    )
