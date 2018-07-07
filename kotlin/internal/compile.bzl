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

def _kotlin_do_compile_action(ctx, rule_kind, output_jar, compile_jars, module_name, friend_paths, srcs):
    """Internal macro that sets up a Kotlin compile action.

    This macro only supports a single Kotlin compile operation for a rule.

    Args:
      rule_kind: The rule kind,
      output_jar: The jar file that this macro will use as the output of the action.
      compile_jars: The compile time jars provided on the classpath for the compile operations -- callers are
        responsible for preparing the classpath. The stdlib (and jdk7 + jdk8) should generally be added to the classpath
        by the caller -- kotlin-reflect could be optional.
    """
    classes_directory=_declare_output_directory(ctx, "jvm", "classes")
    generated_classes_directory=_declare_output_directory(ctx, "jvm", "generated_classes")
    sourcegen_directory=_declare_output_directory(ctx, "jvm", "sourcegenfiles")
    temp_directory=_declare_output_directory(ctx, "jvm", "temp")

    toolchain=ctx.toolchains[kt.defs.TOOLCHAIN_TYPE]

    args = [
        "--target_label", ctx.label,
        "--rule_kind", rule_kind,

        "--classdir", classes_directory.path,
        "--sourcegendir", sourcegen_directory.path,
        "--tempdir", temp_directory.path,
        "--kotlin_generated_classdir", generated_classes_directory.path,

        "--output", output_jar.path,
        "--output_jdeps", ctx.outputs.jdeps.path,
        "--classpath", "\n".join([f.path for f in compile_jars.to_list()]),
        "--kotlin_friend_paths", "\n".join(friend_paths.to_list()),
        "--kotlin_jvm_target", toolchain.jvm_target,
        "--kotlin_api_version", toolchain.api_version,
        "--kotlin_language_version", toolchain.language_version,
        "--kotlin_module_name", module_name,
        "--kotlin_passthrough_flags", "-Xcoroutines=%s" % toolchain.coroutines
    ]

    if (len(srcs.kt) + len(srcs.java)) > 0:
        args += ["--sources", "\n".join([s.path for s in (srcs.kt + srcs.java)])]

    if len(srcs.src_jars) > 0:
        args += ["--source_jars", "\n".join([sj.path for sj in srcs.src_jars])]

    # Collect and prepare plugin descriptor for the worker.
    plugin_info=plugins.merge_plugin_infos(ctx.attr.plugins + ctx.attr.deps)
    if len(plugin_info.annotation_processors) > 0:
        args += [ "--kt-plugins", plugin_info.to_json() ]

    # Declare and write out argument file.
    args_file = ctx.actions.declare_file(ctx.label.name + ".jar-2.params")
    ctx.actions.write(args_file, "\n".join(args))

    progress_message = "Compiling Kotlin %s { kt: %d, java: %d, srcjars: %d }" % (
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars)
    )

    inputs, _, input_manifests = ctx.resolve_command(tools = [toolchain.kotlinbuilder])
    ctx.actions.run(
        mnemonic = "KotlinCompile",
        inputs = depset([args_file]) + inputs + ctx.files.srcs + compile_jars,
        outputs = [
            output_jar,
            ctx.outputs.jdeps,
            sourcegen_directory,
            classes_directory,
            temp_directory,
            generated_classes_directory
        ],
        executable = toolchain.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = ["@" + args_file.path],
        progress_message = progress_message,
        input_manifests = input_manifests
    )

def _select_std_libs(ctx):
    return ctx.toolchains[kt.defs.TOOLCHAIN_TYPE].jvm_stdlibs

def _make_java_provider(ctx, srcs, input_deps=[], auto_deps=[]):
    """Creates the java_provider for a Kotlin target.

    This macro is distinct from the kotlin_make_providers as collecting the java_info is useful before the DefaultInfo is
    created.

    Args:
    ctx: The ctx of the rule in scope when this macro is called. The macro will pick up the following entities from
      the rule ctx:
        * The default output jar.
        * The `deps` for this provider.
        * Optionally `exports` (see java rules).
        * The `_kotlin_runtime` implicit dependency.
    Returns:
    A JavaInfo provider.
    """
    runtime = ctx.toolchains[kt.defs.TOOLCHAIN_TYPE].jvm_runtime
    deps=utils.collect_all_jars(input_deps)
    exported_deps=utils.collect_all_jars(getattr(ctx.attr, "exports", []))

    my_compile_jars = exported_deps.compile_jars + [ctx.outputs.jar]
    my_runtime_jars = exported_deps.transitive_runtime_jars + [ctx.outputs.jar]

    my_transitive_compile_jars = my_compile_jars + deps.transitive_compile_time_jars + exported_deps.transitive_compile_time_jars + auto_deps
    my_transitive_runtime_jars = my_runtime_jars + deps.transitive_runtime_jars + exported_deps.transitive_runtime_jars + runtime + auto_deps

    # collect the runtime jars from the runtime_deps attribute.
    for jar in ctx.attr.runtime_deps:
        my_transitive_runtime_jars += jar[JavaInfo].transitive_runtime_jars

    return java_common.create_provider(
        use_ijar = False,
        # A list or set of output source jars that contain the uncompiled source files including the source files
        # generated by annotation processors if the case.
        source_jars = utils.actions.maybe_make_srcsjar(ctx, srcs),
        # A list or a set of jars that should be used at compilation for a given target.
        compile_time_jars = my_compile_jars,
        # A list or a set of jars that should be used at runtime for a given target.
        runtime_jars = my_runtime_jars,
        transitive_compile_time_jars = my_transitive_compile_jars,
        transitive_runtime_jars = my_transitive_runtime_jars
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
              source_jars = java_info.source_jars
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

    kotlin_auto_deps=_select_std_libs(ctx)

    deps = ctx.attr.deps + getattr(ctx.attr, "friends", [])

    srcs = utils.partition_srcs(ctx.files.srcs)

    if (len(srcs.kt) + len(srcs.java) == 0) and len(srcs.src_jars) == 0:
        fail("no sources provided")

    # setup the compile action.
    _kotlin_do_compile_action(
        ctx,
        rule_kind = rule_kind,
        output_jar = kt_compile_output_jar,
        compile_jars = utils.collect_jars_for_compile(deps) + kotlin_auto_deps,
        module_name = module_name,
        friend_paths = friend_paths,
        srcs = srcs
    )

    # setup the merge action if needed.
    if len(output_merge_list) > 0:
        utils.actions.fold_jars(ctx, output_jar, output_merge_list)

    # create the java provider but the kotlin and default provider cannot be created here.
    return _make_java_provider(ctx, srcs, deps, kotlin_auto_deps)

compile = struct(
    compile_action = _compile_action,
    make_providers = _make_providers,
)
