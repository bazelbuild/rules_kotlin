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
load("//kotlin/internal:kt.bzl", "kt")
load("//kotlin/internal:plugins.bzl", "plugins")
load("//kotlin/internal:utils.bzl", "utils")

def _declare_output_directory(ctx, aspect, dir_name):
    return ctx.actions.declare_directory("_kotlinc/%s_%s/%s_%s" % (ctx.label.name, aspect, ctx.label.name, dir_name))

def _common_init_args(ctx, rule_kind, module_name):
    toolchain=ctx.toolchains[kt.defs.TOOLCHAIN_TYPE]

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always=True)

    args.add("--target_label", ctx.label)
    args.add("--rule_kind", rule_kind)
    args.add("--kotlin_module_name", module_name)

    args.add("--kotlin_jvm_target", toolchain.jvm_target)
    args.add("--kotlin_api_version", toolchain.api_version)
    args.add("--kotlin_language_version", toolchain.language_version)
    args.add("--kotlin_passthrough_flags", "-Xcoroutines=%s" % toolchain.coroutines)

    return args

def _kotlin_do_compile_action(ctx, rule_kind, output_jar, compile_jars, module_name, friend_paths, srcs):
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
    classes_directory=_declare_output_directory(ctx, "jvm", "classes")
    generated_classes_directory=_declare_output_directory(ctx, "jvm", "generated_classes")
    sourcegen_directory=_declare_output_directory(ctx, "jvm", "sourcegenfiles")
    temp_directory=_declare_output_directory(ctx, "jvm", "temp")

    toolchain=ctx.toolchains[kt.defs.TOOLCHAIN_TYPE]
    args = _common_init_args(ctx, rule_kind, module_name)

    args.add("--classdir", classes_directory)
    args.add("--sourcegendir", sourcegen_directory)
    args.add("--tempdir", temp_directory)
    args.add("--kotlin_generated_classdir", generated_classes_directory)

    args.add("--output", output_jar)
    args.add("--kotlin_output_jdeps", ctx.outputs.jdeps)
    args.add("--kotlin_output_srcjar", ctx.outputs.srcjar)

    args.add("--kotlin_friend_paths", "\n".join(friend_paths.to_list()))

    args.add("--classpath", compile_jars)
    args.add_all("--sources", srcs.all_srcs, omit_if_empty=True)
    args.add_all("--source_jars", srcs.src_jars, omit_if_empty=True)

    # Collect and prepare plugin descriptor for the worker.
    plugin_info=plugins.merge_plugin_infos(ctx.attr.plugins + ctx.attr.deps)
    if len(plugin_info.annotation_processors) > 0:
        args.add("--kotlin_plugins", plugin_info.to_json())

    progress_message = "Compiling Kotlin %s { kt: %d, java: %d, srcjars: %d }" % (
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars)
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
            generated_classes_directory
        ],
        executable = toolchain.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [args],
        progress_message = progress_message,
        input_manifests = input_manifests
    )

def _make_providers(ctx, java_info, module_name, transitive_files=depset(order="default")):
    kotlin_info=kt.info.KtInfo(
        srcs=ctx.files.srcs,
        module_name = module_name,
        # intelij aspect needs this.
        outputs = struct(
            jdeps = ctx.outputs.jdeps,
            jars = [struct(
              class_jar = ctx.outputs.jar,
              ijar = None,
              source_jars = [ctx.outputs.srcjar]
            )]
        ),
    )

    default_info = DefaultInfo(
        files=depset([ctx.outputs.jar]),
        runfiles=ctx.runfiles(
            transitive_files=transitive_files,
            collect_default=True
        ),
    )

    return struct(
        kt=kotlin_info,
        providers=[java_info,default_info,kotlin_info],
    )

def _compile_action(ctx, rule_kind, module_name, friend_paths=depset(), src_jars=[]):
    """Setup a kotlin compile action.

    Args:
        ctx: The rule context.
    Returns:
        A JavaInfo struct for the output jar that this macro will build.
    """
    # The main output jars
    output_jar = ctx.outputs.jar

    # The output of the compile step may be combined (folded) with other entities -- e.g., other class files from annotation processing, embedded resources.
    kt_compile_output_jar=output_jar
    # the list of jars to merge into the final output, start with the resource jars if any were provided.
    output_merge_list=ctx.files.resource_jars

    # If this rule has any resources declared setup a zipper action to turn them into a jar and then add the declared zipper output to the merge list.
    if len(ctx.files.resources) > 0:
        output_merge_list = output_merge_list + [utils.actions.build_resourcejar(ctx)]

    # If this compile operation requires merging other jars setup the compile operation to go to a intermediate file and add that file to the merge list.
    if len(output_merge_list) > 0:
        # Intermediate jar containing the Kotlin compile output.
        kt_compile_output_jar=ctx.new_file(ctx.label.name + "-ktclass.jar")
        # If we setup indirection than the first entry in the merge list is the result of the kotlin compile action.
        output_merge_list=[ kt_compile_output_jar ] + output_merge_list

    srcs = utils.partition_srcs(ctx.files.srcs)

    if (len(srcs.kt) + len(srcs.java) == 0) and len(srcs.src_jars) == 0:
        fail("no sources provided")

    toolchain=ctx.toolchains[kt.defs.TOOLCHAIN_TYPE]

    deps = [
        d[JavaInfo]
        for d in (
            getattr(ctx.attr, "friends", []) +
            ctx.attr.deps
        )
    ] + [toolchain.jvm_stdlibs]

    # setup the compile action.
    _kotlin_do_compile_action(
        ctx,
        rule_kind = rule_kind,
        output_jar = kt_compile_output_jar,
        compile_jars = java_common.merge(deps).compile_jars,
        module_name = module_name,
        friend_paths = friend_paths,
        srcs = srcs
    )

    # setup the merge action if needed.
    if len(output_merge_list) > 0:
        utils.actions.fold_jars(ctx, rule_kind, output_jar, output_merge_list)

    # create the java provider but the kotlin and default provider cannot be created here.
    return JavaInfo(
        output_jar = ctx.outputs.jar,
        compile_jar = ctx.outputs.jar,
        source_jar = ctx.outputs.srcjar,
#        jdeps = ctx.outputs.jdeps,
        deps = deps,
        runtime_deps = [d[JavaInfo] for d in ctx.attr.runtime_deps],
        exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
        neverlink = getattr(ctx.attr, "neverlink", False)
    )

compile = struct(
    compile_action = _compile_action,
    make_providers = _make_providers,
)
