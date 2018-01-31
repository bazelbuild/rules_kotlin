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
    "//kotlin/rules:defs.bzl",
    _KotlinInfo = "KotlinInfo",
)
load(
    "//kotlin/rules:util.bzl",
    _collect_all_jars = "collect_all_jars",
    _collect_jars_for_compile = "collect_jars_for_compile",
    _kotlin_build_resourcejar_action = "kotlin_build_resourcejar_action",
    _kotlin_fold_jars_action = "kotlin_fold_jars_action",
    _kotlin_maybe_make_srcs_action = "kotlin_maybe_make_srcs_action",
)

def _kotlin_do_compile_action(ctx, output_jar, compile_jars, opts):
    """Internal macro that sets up a Kotlin compile action.

    This macro only supports a single Kotlin compile operation for a rule.

    Args:
      ctx: the ctx of the rule in scope when this macro is called. The macro will pick up the following entities from
        the rule ctx:
          * The `srcs` to compile.
      output_jar: The jar file that this macro will use as the output of the action -- a hardcoded default output is not
        setup by this rule as this would close the door to optimizations for simple compile operations.
      compile_jars: The compile time jars provided on the classpath for the compile operations -- callers are
        responsible for preparing the classpath. The stdlib (and jdk7 + jdk8) should generally be added to the classpath
        by the caller -- kotlin-reflect could be optional.
      opts: struct containing Kotlin compilation options.
    """
    args = [
        "--label", ctx.label,
        "--output_classjar", output_jar.path,
        "--output_jdeps", ctx.outputs.jdeps.path,
        "--classpath", ":".join([f.path for f in compile_jars.to_list()]),
        "--sources", ":".join([f.path for f in ctx.files.srcs]),
        # https://github.com/hsyed/rules_kotlin/issues/3.
        "--kotlin_jvm_target", "1.8", "--kotlin_api_version", "1.2", "--kotlin_language_version", "1.2"
    ]

    # re-enable compilation options https://github.com/hsyed/rules_kotlin/issues/3.
#    for k, v in ctx.attr.opts.items():
#        args + [ "-%s" % k, v]:

    # Advanced options
#    args += ["-X%s" % opt for opt in ctx.attr.x_opts]
#
#
#    # Plugin options
#    for k, v in ctx.attr.plugin_opts.items():
#        args += ["-P"]
#        args += ["plugin:%s=\"%s\"" % (k, v)]

    # Declare and write out argument file.
    args_file = ctx.actions.declare_file(ctx.label.name + "-worker.args")
    ctx.actions.write(args_file, "\n".join(args))

    # When a stratetegy isn't provided for the worker and the workspace is fresh then certain deps are not available under
    # external/@com_github_jetbrains_kotlin/... that is why the classpath is added explicetly.
    compile_inputs = (
      depset([args_file]) +
      ctx.files.srcs +
      compile_jars +
      ctx.files._kotlin_compiler_classpath +
      ctx.files._jdk)

    ctx.action(
        mnemonic = "KotlinCompile",
        inputs = compile_inputs,
        outputs = [output_jar, ctx.outputs.jdeps],
        executable = ctx.executable._kotlinw,
        execution_requirements = {"supports-workers": "1"},
        arguments = ["@" + args_file.path],
        progress_message="Compiling %d Kotlin source files to %s" % (len(ctx.files.srcs), output_jar.short_path),
    )

def _select_compilation_options(ctx):
  """TODO Stub: setup compilation options"""
  return struct(
      # Basic kotlin compile options.
      opts = {},
      # Advanced Kotlin compile options.
      x_opts ={},
      # Kotlin compiler plugin options.
      plugin_opts = {}
  )

def _select_std_libs(ctx):
    return ctx.files._kotlin_std

def _make_java_provider(ctx, auto_deps=[]):
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
    deps=_collect_all_jars(ctx.attr.deps)
    exported_deps=_collect_all_jars(getattr(ctx.attr, "exports", []))

    my_compile_jars = exported_deps.compile_jars + [ctx.outputs.jar]
    my_runtime_jars = exported_deps.runtime_jars + [ctx.outputs.jar]

    my_transitive_compile_jars = my_compile_jars + deps.transitive_compile_time_jars + exported_deps.transitive_compile_time_jars + auto_deps
    my_transitive_runtime_jars = my_runtime_jars + deps.transitive_runtime_jars + exported_deps.transitive_runtime_jars + [ctx.file._kotlin_runtime] + auto_deps

    # collect the runtime jars from the runtime_deps attribute.
    for jar in ctx.attr.runtime_deps:
        my_transitive_runtime_jars += jar[JavaInfo].transitive_runtime_jars

    return java_common.create_provider(
        use_ijar = False,
        # A list or set of output source jars that contain the uncompiled source files including the source files
        # generated by annotation processors if the case.
        source_jars=_kotlin_maybe_make_srcs_action(ctx),
        # A list or a set of jars that should be used at compilation for a given target.
        compile_time_jars = my_compile_jars,
        # A list or a set of jars that should be used at runtime for a given target.
        runtime_jars=my_runtime_jars,
        transitive_compile_time_jars= my_transitive_compile_jars,
        transitive_runtime_jars=my_transitive_runtime_jars
    )

def kotlin_make_providers(ctx, java_info, transitive_files=depset(order="default")):
    kotlin_info=_KotlinInfo(
        src=ctx.attr.srcs,
        outputs = struct(
            jdeps = ctx.outputs.jdeps,
            jars = [struct(
              class_jar = ctx.outputs.jar,
              ijar = None,
            )]
        ), # intelij aspect needs this.
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

def kotlin_compile_action(ctx):
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
        output_merge_list = output_merge_list + [_kotlin_build_resourcejar_action(ctx)]

    # If this compile operation requires merging other jars setup the compile operation to go to a intermediate file and add that file to the merge list.
    if len(output_merge_list) > 0:
        # Intermediate jar containing the Kotlin compile output.
        kt_compile_output_jar=ctx.new_file(ctx.label.name + "-ktclass.jar")
        # If we setup indirection than the first entry in the merge list is the result of the kotlin compile action.
        output_merge_list=[ kt_compile_output_jar ] + output_merge_list

    kotlin_auto_deps=_select_std_libs(ctx)

    # setup the compile action.
    _kotlin_do_compile_action(
        ctx,
        kt_compile_output_jar,
        _collect_jars_for_compile(ctx.attr.deps) + kotlin_auto_deps,
        _select_compilation_options(ctx)
    )
    # setup the merge action if needed.
    if len(output_merge_list) > 0:
        _kotlin_fold_jars_action(ctx, output_jar, output_merge_list)

    # create the java provider but the kotlin and default provider cannot be created here.
    return _make_java_provider(ctx, kotlin_auto_deps)