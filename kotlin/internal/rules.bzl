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

load("//kotlin/internal:compile.bzl", "compile")
load("//kotlin/internal:kt.bzl", "kt")
load("//kotlin/internal:utils.bzl", "utils")

def _extract_kotlin_artifact(files):
    jars = [j for j in files if j.basename.endswith(".jar") and not j.basename.endswith("-sources.jar")]
    srcjars = [j for j in files if j.basename.endswith("-sources.jar")]
    if len(jars) != 1:
        fail("a single classjar target must be available in jars")
    srcjar = None
    if len(srcjars) == 1:
        srcjar=srcjars[0]
    return struct(class_jar = jars[0], source_jar=srcjar, ijar = None)

def _collect_import_artifacts(ctx):
    artifacts = [_extract_kotlin_artifact(a.files) for a in ctx.attr.jars]
    if len(artifacts) == 1:
        if len(ctx.files.srcjar) == 1:
            if artifacts[0].source_jar != None:
                fail("the jars attr allready provided a relevant *-sources.jar")
            else:
                artifacts = [struct(class_jar=artifacts[0].class_jar, source_jar = ctx.file.srcjar, ijar = None)]
    if len(artifacts) > 1 and ctx.file.srcjar != None:
        fail("the srcjar attribute should not be set when importing multiple class jars")
    return artifacts

def kt_jvm_import_impl(ctx):
    artifacts=_collect_import_artifacts(ctx)

    jars = [a.class_jar for a in artifacts]

    java_info = java_common.create_provider(
        use_ijar = False,
        source_jars=[a.source_jar for a in artifacts],
        compile_time_jars = jars,
        runtime_jars= jars,
        transitive_compile_time_jars=jars,
        transitive_runtime_jars=jars
    )
    kotlin_info=kt.info.KtInfo(outputs = struct(jars = artifacts))
    default_info = DefaultInfo(files=depset(jars))
    return struct(kt = kotlin_info, providers= [default_info, java_info, kotlin_info])

def kt_jvm_library_impl(ctx):
    return compile.make_providers(ctx, compile.compile_action(ctx))

def kt_jvm_binary_impl(ctx):
    java_info = compile.compile_action(ctx)
    utils.actions.write_launcher(
        ctx,
        java_info.transitive_runtime_jars,
        ctx.attr.main_class,
        ctx.attr.jvm_flags
    )
    return compile.make_providers(
        ctx,
        java_info,
        depset(
            order = "default",
            transitive=[java_info.transitive_runtime_jars],
            direct=[ctx.executable._java]
        )
    )

def kt_jvm_junit_test_impl(ctx):
    java_info = compile.compile_action(ctx)

    transitive_runtime_jars = java_info.transitive_runtime_jars + ctx.files._bazel_test_runner
    launcherJvmFlags = ["-ea", "-Dbazel.test_suite=%s"% ctx.attr.test_class]

    utils.actions.write_launcher(
        ctx,
        transitive_runtime_jars,
        main_class = "com.google.testing.junit.runner.BazelTestRunner",
        jvm_flags = launcherJvmFlags + ctx.attr.jvm_flags,
    )
    return compile.make_providers(
        ctx,
        java_info,
        depset(
            order = "default",
            transitive=[transitive_runtime_jars],
            direct=[ctx.executable._java]
        )
    )