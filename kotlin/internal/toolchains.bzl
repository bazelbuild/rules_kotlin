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
    "//kotlin/internal:opts.bzl",
    "JavacOptions",
    "KotlincOptions",
    "kt_javac_options",
    "kt_kotlinc_options",
)
load(
    "//kotlin/internal:defs.bzl",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
    _KtJsInfo = "KtJsInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)

"""Kotlin Toolchains

This file contains macros for defining and registering specific toolchains.

### Examples

To override a tool chain use the appropriate macro in a `BUILD` file to declare the toolchain:

```bzl
load("@io_bazel_rules_kotlin//kotlin:toolchains.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name= "custom_toolchain",
    api_version = "1.6",
    language_version = "1.6",
)
```
and then register it in the `WORKSPACE`:
```bzl
register_toolchains("//:custom_toolchain")
```
"""

def _kotlin_toolchain_impl(ctx):
    compile_time_providers = [
        JavaInfo(
            output_jar = jar,
            compile_jar = jar,
            neverlink = True,
        )
        for jar in ctx.files.jvm_stdlibs
    ]
    runtime_providers = [
        JavaInfo(
            output_jar = jar,
            compile_jar = jar,
        )
        for jar in ctx.files.jvm_runtime
    ]

    toolchain = dict(
        language_version = ctx.attr.language_version,
        api_version = ctx.attr.api_version,
        debug = ctx.attr.debug,
        jvm_target = ctx.attr.jvm_target,
        kotlinbuilder = ctx.attr.kotlinbuilder,
        jdeps_merger = ctx.attr.jdeps_merger,
        kotlin_home = ctx.attr.kotlin_home,
        jvm_stdlibs = java_common.merge(compile_time_providers + runtime_providers),
        js_stdlibs = ctx.attr.js_stdlibs,
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1" if ctx.attr.experimental_multiplex_workers else "0",
        },
        experimental_use_abi_jars = ctx.attr.experimental_use_abi_jars,
        experimental_strict_kotlin_deps = ctx.attr.experimental_strict_kotlin_deps,
        experimental_report_unused_deps = ctx.attr.experimental_report_unused_deps,
        experimental_reduce_classpath_mode = ctx.attr.experimental_reduce_classpath_mode,
        javac_options = ctx.attr.javac_options[JavacOptions] if ctx.attr.javac_options else None,
        kotlinc_options = ctx.attr.kotlinc_options[KotlincOptions] if ctx.attr.kotlinc_options else None,
        empty_jar = ctx.file._empty_jar,
        empty_jdeps = ctx.file._empty_jdeps,
        jacocorunner = ctx.attr.jacocorunner,
    )

    return [
        platform_common.ToolchainInfo(**toolchain),
    ]

_kt_toolchain = rule(
    doc = """The kotlin toolchain. This should not be created directly `define_kt_toolchain` should be used. The
    rules themselves define the toolchain using that macro.""",
    attrs = {
        "kotlin_home": attr.label(
            doc = "the filegroup defining the kotlin home",
            default = Label("@" + _KT_COMPILER_REPO + "//:home"),
            allow_files = True,
        ),
        "kotlinbuilder": attr.label(
            doc = "the kotlin builder executable",
            default = Label("//src/main/kotlin:build"),
            executable = True,
            allow_files = True,
            cfg = "exec",
        ),
        "jdeps_merger": attr.label(
            doc = "the jdeps merger executable",
            default = Label("//src/main/kotlin:jdeps_merger"),
            executable = True,
            allow_files = True,
            cfg = "exec",
        ),
        "language_version": attr.string(
            doc = "this is the -language_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html)",
            default = "1.8",
            values = [
                "1.1",
                "1.2",
                "1.3",
                "1.4",
                "1.5",
                "1.6",
                "1.7",
            ],
        ),
        "api_version": attr.string(
            doc = "this is the -api_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html).",
            default = "1.8",
            values = [
                "1.1",
                "1.2",
                "1.3",
                "1.4",
                "1.5",
                "1.6",
                "1.7",
                "1.8",
            ],
        ),
        "debug": attr.string_list(
            doc = """Debugging tags passed to the builder. Two tags are supported. `timings` will cause the builder to
            print timing information. `trace` will cause the builder to print tracing messages. These tags can be
            enabled via the defines `kt_timings=1` and `kt_trace=1`. These can also be enabled on a per target bases by
            using `tags` attribute defined directly on the rules.""",
            allow_empty = True,
        ),
        "jvm_runtime": attr.label_list(
            doc = "The implicit jvm runtime libraries. This is internal.",
            default = [
                Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib"),
            ],
            providers = [JavaInfo],
            cfg = "target",
        ),
        "jvm_stdlibs": attr.label_list(
            doc = "The jvm stdlibs. This is internal.",
            default = [
                Label("@" + _KT_COMPILER_REPO + "//:annotations"),
                Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib"),
                Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib-jdk7"),
                # JDK8 is being added blindly but I think we will probably not support bytecode levels 1.6 when the
                # repo stabelizes so this should be fine.
                Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib-jdk8"),
            ],
            providers = [JavaInfo],
            cfg = "target",
        ),
        "jvm_target": attr.string(
            doc = "the -jvm_target flag. This is only tested at 1.8.",
            default = "1.8",
            values = [
                "1.6",
                "1.8",
                "9",
                "10",
                "11",
                "12",
                "13",
                "15",
                "16",
                "17",
            ],
        ),
        "js_target": attr.string(
            default = "v5",
            values = ["v5"],
        ),
        "js_stdlibs": attr.label_list(
            default = [
                Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib-js"),
            ],
            providers = [_KtJsInfo],
        ),
        "experimental_multiplex_workers": attr.bool(
            doc = """Run workers in multiplex mode.""",
            default = False,
        ),
        "experimental_use_abi_jars": attr.bool(
            doc = """Compile using abi jars. Can be disabled for an individual target using the tag
            `kt_abi_plugin_incompatible`""",
            default = False,
        ),
        "experimental_strict_kotlin_deps": attr.string(
            doc = "Report strict deps violations",
            default = "off",
            values = [
                "off",
                "warn",
                "error",
            ],
        ),
        "experimental_report_unused_deps": attr.string(
            doc = "Report unused dependencies",
            default = "off",
            values = [
                "off",
                "warn",
                "error",
            ],
        ),
        "experimental_reduce_classpath_mode": attr.string(
            doc = "Removes unneeded dependencies from the classpath",
            default = "NONE",
            values = [
                "NONE",
                "KOTLINBUILDER_REDUCED",
            ],
        ),
        "javac_options": attr.label(
            doc = "Compiler options for javac",
            providers = [JavacOptions],
        ),
        "_empty_jar": attr.label(
            doc = """Empty jar for exporting JavaInfos.""",
            allow_single_file = True,
            cfg = "target",
            default = Label("//third_party:empty.jar"),
        ),
        "kotlinc_options": attr.label(
            doc = "Compiler options for kotlinc",
            providers = [KotlincOptions],
        ),
        "_empty_jdeps": attr.label(
            doc = """Empty jdeps for exporting JavaInfos.""",
            allow_single_file = True,
            cfg = "target",
            default = Label("//third_party:empty.jdeps"),
        ),
        "jacocorunner": attr.label(
            default = Label("@bazel_tools//tools/jdk:JacocoCoverage"),
        ),
    },
    implementation = _kotlin_toolchain_impl,
    provides = [platform_common.ToolchainInfo],
)

_KT_DEFAULT_TOOLCHAIN = Label("//kotlin/internal:default_toolchain")

def kt_register_toolchains():
    """This macro registers the kotlin toolchain."""
    native.register_toolchains(str(_KT_DEFAULT_TOOLCHAIN))

# Evaluating the select in the context of bzl file to get its repository
_DEBUG_SELECT = select({
    str(Label("//kotlin/internal:builder_debug_trace")): ["trace"],
    "//conditions:default": [],
}) + select({
    str(Label("//kotlin/internal:builder_debug_timings")): ["timings"],
    "//conditions:default": [],
})

# Evaluating the labels in the context of bzl file to get its repository
_EXPERIMENTAL_USE_ABI_JARS = str(Label("//kotlin/internal:experimental_use_abi_jars"))
_NOEXPERIMENTAL_USE_ABI_JARS = str(Label("//kotlin/internal:noexperimental_use_abi_jars"))

def define_kt_toolchain(
        name,
        language_version = None,
        api_version = None,
        jvm_target = None,
        experimental_use_abi_jars = False,
        experimental_strict_kotlin_deps = None,
        experimental_report_unused_deps = None,
        experimental_reduce_classpath_mode = None,
        experimental_multiplex_workers = None,
        javac_options = Label("//kotlin/internal:default_javac_options"),
        kotlinc_options = Label("//kotlin/internal:default_kotlinc_options"),
        jacocorunner = None):
    """Define the Kotlin toolchain."""
    impl_name = name + "_impl"

    _kt_toolchain(
        name = impl_name,
        language_version = language_version,
        api_version = api_version,
        jvm_target = jvm_target,
        debug = _DEBUG_SELECT,
        experimental_use_abi_jars = select({
            _EXPERIMENTAL_USE_ABI_JARS: True,
            _NOEXPERIMENTAL_USE_ABI_JARS: False,
            "//conditions:default": experimental_use_abi_jars,
        }),
        experimental_multiplex_workers = experimental_multiplex_workers,
        experimental_strict_kotlin_deps = experimental_strict_kotlin_deps,
        experimental_report_unused_deps = experimental_report_unused_deps,
        experimental_reduce_classpath_mode = experimental_reduce_classpath_mode,
        javac_options = javac_options,
        kotlinc_options = kotlinc_options,
        visibility = ["//visibility:public"],
        jacocorunner = jacocorunner,
    )
    native.toolchain(
        name = name,
        toolchain_type = _TOOLCHAIN_TYPE,
        toolchain = impl_name,
        visibility = ["//visibility:public"],
    )

def kt_configure_toolchains():
    """
    Defines the toolchain_type and default toolchain for kotlin compilation.

    Must be called in kotlin/internal/BUILD.bazel
    """
    if native.package_name() != "kotlin/internal":
        fail("kt_configure_toolchains must be called in kotlin/internal not %s" % native.package_name())

    kt_kotlinc_options(
        name = "default_kotlinc_options",
        visibility = ["//visibility:public"],
    )

    kt_javac_options(
        name = "default_javac_options",
        visibility = ["//visibility:public"],
    )

    native.config_setting(
        name = "experimental_use_abi_jars",
        values = {"define": "experimental_use_abi_jars=1"},
        visibility = ["//visibility:public"],
    )
    native.config_setting(
        name = "noexperimental_use_abi_jars",
        values = {"define": "experimental_use_abi_jars=0"},
        visibility = ["//visibility:public"],
    )

    native.config_setting(
        name = "builder_debug_timings",
        values = {"define": "kt_timings=1"},
        visibility = ["//visibility:public"],
    )

    native.config_setting(
        name = "experimental_multiplex_workers",
        values = {"define": "kt_multiplex=1"},
        visibility = ["//visibility:public"],
    )

    native.config_setting(
        name = "builder_debug_trace",
        values = {"define": "kt_trace=1"},
        visibility = ["//visibility:public"],
    )

    native.toolchain_type(
        name = "kt_toolchain_type",
        visibility = ["//visibility:public"],
    )

    define_kt_toolchain(
        name = "default_toolchain",
    )
