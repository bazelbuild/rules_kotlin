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
    "//kotlin/rules:compile.bzl",
    _kotlin_compile_action = "kotlin_compile_action",
    _kotlin_make_providers = "kotlin_make_providers",
)
load(
    "//kotlin/rules:util.bzl",
    _kotlin_write_launcher_action = "kotlin_write_launcher_action",
)

def kotlin_library_impl(ctx):
  return _kotlin_make_providers(ctx, _kotlin_compile_action(ctx))

def kotlin_binary_impl(ctx):
    java_info = _kotlin_compile_action(ctx)
    _kotlin_write_launcher_action(
        ctx,
        java_info.transitive_runtime_jars,
        ctx.attr.main_class,
        ctx.attr.jvm_flags
    )
    return _kotlin_make_providers(
        ctx,
        java_info,
        depset(
            order = "default",
            transitive=[java_info.transitive_runtime_jars],
            direct=[ctx.executable._java]
        )
    )

def kotlin_junit_test_impl(ctx):
    java_info = _kotlin_compile_action(ctx)

    transitive_runtime_jars = java_info.transitive_runtime_jars + ctx.files._bazel_test_runner
    launcherJvmFlags = ["-ea", "-Dbazel.test_suite=%s"% ctx.attr.test_class]

    _kotlin_write_launcher_action(
        ctx,
        transitive_runtime_jars,
        main_class = "com.google.testing.junit.runner.BazelTestRunner",
        jvm_flags = launcherJvmFlags + ctx.attr.jvm_flags,
    )
    return _kotlin_make_providers(
        ctx,
        java_info,
        depset(
            order = "default",
            transitive=[transitive_runtime_jars],
            direct=[ctx.executable._java]
        )
    )