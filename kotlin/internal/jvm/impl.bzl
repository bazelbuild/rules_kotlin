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
load("//kotlin/internal/jvm:compile.bzl", _kt_jvm_produce_jar_actions = "kt_jvm_produce_jar_actions")
load("//kotlin/internal:defs.bzl", _KtJvmInfo = "KtJvmInfo")

def _make_providers(ctx, providers, transitive_files = depset(order = "default")):
    return struct(
        kt = providers.kt,
        providers = [
            providers.java,
            providers.kt,
            DefaultInfo(
                files = depset([ctx.outputs.jar]),
                runfiles = ctx.runfiles(
                    transitive_files = transitive_files,
                    collect_default = True,
                ),
            ),
        ],
    )

def _write_launcher_action(ctx, rjars, main_class, jvm_flags, args = "", wrapper_preamble = ""):
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

def kt_jvm_import_impl(ctx):
    jars = depset()
    source_jars = depset()
    runtime_jars = depset()
    transitive_compile_time_jars = depset()
    transitive_runtime_jars = depset()

    if ctx.file.srcjar:
        source_jars += [ctx.file.srcjar]

    if hasattr(ctx.attr, "runtime_deps"):
        for jar in ctx.attr.runtime_deps:
            transitive_runtime_jars += jar[JavaInfo].transitive_runtime_jars

    for jar in ctx.attr.jars:
        if JavaInfo in jar:
            jars += jar[JavaInfo].full_compile_jars
            source_jars += jar[JavaInfo].transitive_source_jars
            transitive_compile_time_jars += jar[JavaInfo].transitive_compile_time_jars
            transitive_runtime_jars += jar[JavaInfo].transitive_runtime_jars
        else:
            for file in jar.files:
                if file.basename.endswith("-sources.jar"):
                    source_jars += [file]
                elif file.basename.endswith(".jar"):
                    jars += [file]
                else:
                    fail("a jar pointing to a filegroup must either end with -sources.jar or .jar")

    runtime_jars += jars
    transitive_compile_time_jars += jars
    transitive_runtime_jars += jars

    java_info = java_common.create_provider(
        use_ijar = False,
        source_jars = source_jars,
        compile_time_jars = jars,
        runtime_jars = runtime_jars,
        transitive_compile_time_jars = transitive_compile_time_jars,
        transitive_runtime_jars = transitive_runtime_jars,
    )

    # This is needed for intellij plugin, try to pair up jars with their sources so that the sources are mounted
    # correctly.
    source_tally = {}
    for sj in source_jars.to_list():
        if sj.basename.endswith("-sources.jar"):
            source_tally[sj.basename.replace("-sources.jar", ".jar")] = sj
    artifacts = []
    for jar in jars.to_list():
        if jar.basename in source_tally:
            artifacts += [struct(class_jar = jar, source_jar = source_tally[jar.basename], ijar = None)]
        else:
            artifacts += [struct(class_jar = jar, ijar = None)]

    kotlin_info = _KtJvmInfo(outputs = struct(jars = artifacts))
    default_info = DefaultInfo(files = depset(jars))
    return struct(kt = kotlin_info, providers = [default_info, java_info, kotlin_info])

def kt_jvm_library_impl(ctx):
    return _make_providers(
        ctx,
        _kt_jvm_produce_jar_actions(ctx, "kt_jvm_library"),
    )

def kt_jvm_binary_impl(ctx):
    providers = _kt_jvm_produce_jar_actions(ctx, "kt_jvm_binary")
    _write_launcher_action(
        ctx,
        providers.java.transitive_runtime_jars,
        ctx.attr.main_class,
        ctx.attr.jvm_flags,
    )
    return _make_providers(
        ctx,
        providers,
        depset(
            order = "default",
            transitive = [providers.java.transitive_runtime_jars],
            direct = [ctx.executable._java],
        ),
    )

def kt_jvm_junit_test_impl(ctx):
    providers = _kt_jvm_produce_jar_actions(ctx, "kt_jvm_test")
    runtime_jars = providers.java.transitive_runtime_jars + ctx.files._bazel_test_runner
    _write_launcher_action(
        ctx,
        runtime_jars,
        main_class = ctx.attr.main_class,
        jvm_flags = ["-ea", "-Dbazel.test_suite=%s" % ctx.attr.test_class] + ctx.attr.jvm_flags,
    )
    return _make_providers(
        ctx,
        providers,
        depset(
            order = "default",
            transitive = [runtime_jars],
            direct = [ctx.executable._java],
        ),
    )
