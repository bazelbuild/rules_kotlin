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
    "//kotlin/internal/jvm:compile.bzl",
    "export_only_providers",
    _kt_jvm_produce_jar_actions = "kt_jvm_produce_jar_actions",
)
load(
    "//kotlin/internal:defs.bzl",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtJvmInfo = "KtJvmInfo",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)
load("//third_party:jarjar.bzl", "jarjar_action")

def _make_providers(ctx, providers, transitive_files = depset(order = "default"), *additional_providers):
    return struct(
        kt = providers.kt,
        providers = [
            providers.java,
            providers.kt,
            DefaultInfo(
                files = depset([ctx.outputs.jar, ctx.outputs.jdeps]),
                runfiles = ctx.runfiles(
                    # explicitly include data files, otherwise they appear to be missing
                    files = ctx.files.data,
                    transitive_files = transitive_files,
                    # continue to use collect_default until proper transitive data collecting is
                    # implmented.
                    collect_default = True,
                ),
            ),
        ] + list(additional_providers),
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
    javabin = "JAVABIN=" + str(ctx.attr._java_runtime[java_common.JavaRuntimeInfo].java_executable_exec_path)

    ctx.actions.expand_template(
        template = template,
        output = ctx.outputs.executable,
        substitutions = {
            "%classpath%": classpath,
            "%java_start_class%": main_class,
            "%javabin%": javabin,
            "%jvm_flags%": jvm_flags,
            "%set_jacoco_metadata%": "",
            "%set_jacoco_main_class%": "",
            "%set_jacoco_java_runfiles_root%": "",
            "%set_java_coverage_new_implementation%": "",
            "%workspace_prefix%": ctx.workspace_name + "/",
        },
        is_executable = True,
    )

def _is_source_jar_stub(jar):
    """Workaround for intellij plugin expecting a source jar"""
    return jar.path.endswith("third_party/empty.jar")

def _unify_jars(ctx):
    if bool(ctx.attr.jar):
        return struct(class_jar = ctx.file.jar, source_jar = ctx.file.srcjar, ijar = None)
    else:
        # Legacy handling.
        jars = []
        source_jars = []
        if (ctx.file.srcjar and not ("%s" % ctx.file.srcjar.path).endswith("third_party/empty.jar")):
            source_jars += [ctx.file.srcjar]

        # There must be a single jar jar and it can either be a filegroup or a JavaInfo.
        for jar in ctx.attr.jars:
            # If a JavaInfo is available it's because it was picked up from a `maven_jar` style attribute -- e.g.,
            # @com_google_guava_guava//jar. so the transitive_compile_jars or the transitive_runtime_jars should not be
            # visited -- descending into these results in ijars entering the graph.
            if JavaInfo in jar:
                jars += jar[JavaInfo].full_compile_jars.to_list()
                source_jars += jar[JavaInfo].transitive_source_jars.to_list()
            else:
                # this branch occurs when the attr was a filegroup.
                for file in jar.files.to_list():
                    if file.basename.endswith("-sources.jar"):
                        source_jars.append(file)
                    elif file.basename.endswith(".jar"):
                        jars.append(file)
                    else:
                        fail("a jar pointing to a filegroup must either end with -sources.jar or .jar")

        if len(jars) > 1:
            fail("Got more than one jar, this is an error create an issue: %s" % jars)
        if len(source_jars) > 1:
            fail("Got more than one source jar. " +
                 "Did you include both srcjar and a sources jar in the jars attribute?: %s" % source_jars)
        return struct(class_jar = jars[0], source_jar = source_jars[0] if len(source_jars) == 1 else None, ijar = None)

def kt_jvm_import_impl(ctx):
    if bool(ctx.attr.jars) and bool(ctx.attr.jar):
        fail("Cannot use both jars= and jar= attribute.  Prefer jar=")

    artifact = _unify_jars(ctx)
    kt_info = _KtJvmInfo(
        module_name = _utils.derive_module_name(ctx),
        exported_compiler_plugins = depset(getattr(ctx.attr, "exported_compiler_plugins", [])),
        outputs = struct(
            jars = [artifact],
        ),
    )
    return struct(
        kt = kt_info,
        providers = [
            DefaultInfo(
                files = depset(direct = [artifact.class_jar]),
                runfiles = ctx.runfiles(files = [artifact.class_jar, artifact.source_jar]),
            ),
            JavaInfo(
                output_jar = artifact.class_jar,
                compile_jar = artifact.class_jar,
                source_jar = artifact.source_jar,
                runtime_deps = [dep[JavaInfo] for dep in ctx.attr.runtime_deps if JavaInfo in dep],
                deps = [dep[JavaInfo] for dep in ctx.attr.deps if JavaInfo in dep],
                exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
                neverlink = getattr(ctx.attr, "neverlink", False),
            ),
            kt_info,
        ],
    )

def kt_jvm_library_impl(ctx):
    if ctx.attr.neverlink and ctx.attr.runtime_deps:
        fail("runtime_deps and neverlink is nonsensical.", attr = "runtime_deps")

    if not ctx.attr.srcs and len(ctx.attr.deps) > 0:
        fail(
            "deps without srcs in invalid." +
            "\nTo add runtime dependencies use runtime_deps." +
            "\nTo export libraries use exports.",
            attr = "deps",
        )

    return _make_providers(
        ctx,
        _kt_jvm_produce_jar_actions(ctx, "kt_jvm_library") if ctx.attr.srcs else export_only_providers(
            ctx = ctx,
            actions = ctx.actions,
            outputs = ctx.outputs,
            attr = ctx.attr,
        ),
    )

def kt_jvm_binary_impl(ctx):
    providers = _kt_jvm_produce_jar_actions(ctx, "kt_jvm_binary")
    _write_launcher_action(
        ctx,
        providers.java.transitive_runtime_jars,
        ctx.attr.main_class,
        ctx.attr.jvm_flags,
    )
    if len(ctx.attr.srcs) == 0 and len(ctx.attr.deps) > 0:
        fail("deps without srcs in invalid. To add runtime classpath and resources, use runtime_deps.", attr = "deps")

    return _make_providers(
        ctx,
        providers,
        depset(
            order = "default",
            transitive = [providers.java.transitive_runtime_jars],
            direct = ctx.files._java_runtime,
        ),
    )

_SPLIT_STRINGS = [
    "src/test/java/",
    "src/test/kotlin/",
    "javatests/",
    "kotlin/",
    "java/",
    "test/",
]

def kt_jvm_junit_test_impl(ctx):
    providers = _kt_jvm_produce_jar_actions(ctx, "kt_jvm_test")
    runtime_jars = depset(ctx.files._bazel_test_runner, transitive = [providers.java.transitive_runtime_jars])

    test_class = ctx.attr.test_class

    # If no test_class, do a best-effort attempt to infer one.
    if not bool(ctx.attr.test_class):
        for file in ctx.files.srcs:
            if file.basename.split(".")[0] == ctx.attr.name:
                for splitter in _SPLIT_STRINGS:
                    elements = file.short_path.split(splitter, 1)
                    if len(elements) == 2:
                        test_class = elements[1].split(".")[0].replace("/", ".")

    _write_launcher_action(
        ctx,
        runtime_jars,
        main_class = ctx.attr.main_class,
        jvm_flags = [
            "-ea",
            "-Dbazel.test_suite=%s" % test_class,
        ] + ctx.attr.jvm_flags,
    )

    return _make_providers(
        ctx,
        providers,
        depset(
            order = "default",
            transitive = [runtime_jars],
            direct = ctx.files._java_runtime,
        ),
        # adds common test variables, including TEST_WORKSPACE.
        testing.TestEnvironment({}),
    )

_KtCompilerPluginInfoDeps = provider(
    fields = {
        "compiler_libs": "list javainfo of a compiler library",
    },
)

def kt_compiler_deps_aspect_impl(target, ctx):
    "Collects the dependencies of a target and forward it to allow deshading the plugin jars."
    return _KtCompilerPluginInfoDeps(
        compiler_libs = [
            t[JavaInfo]
            for d in ["deps", "runtime_deps", "exports"]
            for t in getattr(ctx.rule.attr, d, [])
            if JavaInfo in t
        ],
    )

def kt_compiler_plugin_impl(ctx):
    plugin_id = ctx.attr.id
    options = []
    for (k, v) in ctx.attr.options.items():
        if "=" in k:
            fail("kt_compiler_plugin options keys cannot contain the = symbol")
        options.append(struct(id = plugin_id, value = "%s=%s" % (k, v)))

    jars = []
    compiler_lib_infos = []
    for t in [t for t in ctx.attr.deps]:
        compiler_lib_infos.extend(t[_KtCompilerPluginInfoDeps].compiler_libs)
        ji = t[JavaInfo]
        if ji.outputs:  # can be None, according to the docs.
            for jar_output in ji.outputs.jars:
                jar = jar_output.class_jar
                if ctx.attr.target_embedded_compiler:
                    jars.append(jarjar_action(
                        actions = ctx.actions,
                        jarjar = ctx.executable._jarjar,
                        label = ctx.label,
                        rules = ctx.file._jetbrains_deshade_rules,
                        input = jar,
                        output = ctx.actions.declare_file("%s_deshaded_%s" % (ctx.label.name, jar.basename)),
                    ))
                else:
                    jars.append(jar)

    if not jars:
        fail("Unable to locate plugin jars. Ensure they are exported or the direct result of a library in the deps.")

    if not (ctx.attr.compile_phase or ctx.attr.stubs_phase):
        fail("Plugin must execute during in one or more phases: stubs_phase, compile_phase")

    info = java_common.merge([
        JavaInfo(output_jar = d, compile_jar = d, deps = compiler_lib_infos)
        for d in jars
    ])

    return [
        DefaultInfo(files = depset(jars)),
        java_common.merge([]),
        _KtCompilerPluginInfo(
            plugin_jars = jars,
            classpath = depset(jars, transitive = [i.transitive_deps for i in compiler_lib_infos]),
            options = options,
            stubs = ctx.attr.stubs_phase,
            compile = ctx.attr.compile_phase,
        ),
    ]
