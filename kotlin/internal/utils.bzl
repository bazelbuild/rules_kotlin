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
    "//kotlin/internal:kt.bzl",
    kt = "kt",
)
# MISC UTILS ###################################################################################################################################################
def _restore_label(l):
    lbl = l.workspace_root
    if lbl.startswith("external/"):
        lbl = lbl.replace("external/", "@")
    return lbl + "//" + l.package + ":" + l.name

def _restore_label_for_params(l):
    lbl = l.workspace_root
    if lbl.startswith("external/"):
        lbl = lbl.replace("external/", "@@")
    return lbl + "//" + l.package + ":" + l.name

# DEPSET UTILS #################################################################################################################################################
def _collect_all_jars(deps):
    """
    Merges a list of java providers into a struct of depsets.
    """
    compile_jars = depset()
    runtime_jars = depset()
    transitive_runtime_jars = depset()
    transitive_compile_time_jars = depset()

    for dep_target in deps:
        if JavaInfo in dep_target:
            java_provider = dep_target[JavaInfo]
            compile_jars += java_provider.compile_jars
            # the runtime_jar compile_jar seperation is here to support the exports concept.
            runtime_jars += java_provider.full_compile_jars
            transitive_compile_time_jars += java_provider.transitive_compile_time_jars
            transitive_runtime_jars += java_provider.transitive_runtime_jars

    return struct (
        compile_jars = compile_jars,
        runtime_jars = runtime_jars,
        transitive_runtime_jars = transitive_runtime_jars,
        transitive_compile_time_jars = transitive_compile_time_jars
    )

# RESOURCE JARS ################################################################################################################################################
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
            return  dir_1 + dir_2, rel_path
    return "", path

def _adjust_resources_path(path, resource_strip_prefix):
    if resource_strip_prefix:
      return _adjust_resources_path_by_strip_prefix(path,resource_strip_prefix)
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
            res_path=res_path,
            target_path=target_path,
            c_dir=c_dir)
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
        path=zipper_arg_path.path,
        resources_jar_output=resources_jar_output.path,
        zipper=ctx.executable._zipper.path,
    )
    ctx.action(
        mnemonic="KotlinZipResourceJar",
        inputs=ctx.files.resources + [ctx.executable._zipper,zipper_arg_path],
        outputs=[resources_jar_output],
        command=cmd,
        progress_message="Creating intermediate resource jar %s" % ctx.label,
        arguments=[]
    )
    return resources_jar_output

# SRC JARS #####################################################################################################################################################
def _maybe_make_srcsjar_action(ctx):
    if len(ctx.files.srcs) > 0:
        output_srcjar = ctx.actions.declare_file(ctx.label.name + "-sources.jar")
        args = ["--output", output_srcjar.path]
        for i in ctx.files.srcs:
            args += ["--resources", i.path]

        ctx.action(
            mnemonic = "KotlinPackageSources",
            inputs = ctx.files.srcs,
            outputs = [output_srcjar],
            executable = ctx.executable._singlejar,
            arguments = args,
            progress_message="Creating Kotlin srcjar from %d srcs" % len(ctx.files.srcs),
        )
        return [output_srcjar]
    else:
        return []

# PACKAGE JARS #################################################################################################################################################
def _fold_jars_action(ctx, output_jar, input_jars):
    args=["--output", output_jar.path]
    for i in input_jars:
        args += ["--sources", i.path]

    ctx.action(
        mnemonic = "KotlinFoldOutput",
        inputs = input_jars,
        outputs = [output_jar],
        executable = ctx.executable._singlejar,
        arguments = args,
        progress_message="Merging Kotlin output jar " + output_jar.short_path
    )

# JVM LAUNCH SCRIPTS ###########################################################################################################################################
def _write_launcher_action(ctx, rjars, main_class, jvm_flags, args="", wrapper_preamble=""):
    """Macro that writes out a launcher script shell script.
      Args:
        rjars: All of the runtime jars required to launch this java target.
        main_class: the main class to launch.
        jvm_flags: The flags that should be passed to the jvm.
        args: Args that should be passed to the Binary.
    """
    classpath = ":".join(["${RUNPATH}%s" % (j.short_path) for j in rjars.to_list()])
    jvm_flags = " ".join([ctx.expand_location(f, ctx.attr.data) for f in jvm_flags])
    template = ctx.attr._java_stub_template.files.to_list()[0]

    ctx.actions.expand_template(
        template = template,
        output = ctx.outputs.executable,
        substitutions = {
            "%classpath%": classpath,
            "%java_start_class%": main_class,
            "%javabin%": "JAVABIN=${RUNPATH}" + ctx.executable._java.short_path,
            "%jvm_flags%": jvm_flags,
            "%set_jacoco_metadata%": "",
            "%workspace_prefix%": ctx.workspace_name + "/",
        },
        is_executable = True,
    )

# EXPORT #######################################################################################################################################################
utils = struct(
    actions = struct(
        build_resourcejar = _build_resourcejar_action,
        fold_jars = _fold_jars_action,
        maybe_make_srcsjar = _maybe_make_srcsjar_action,
        write_launcher = _write_launcher_action,
    ),
    collect_all_jars = _collect_all_jars,
    restore_label = _restore_label,
    restore_label_for_params=_restore_label_for_params
)
