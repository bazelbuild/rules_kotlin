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
    _kt_jvm_produce_jar_actions = "kt_jvm_produce_jar_actions",
)
load(
    "//kotlin/internal:defs.bzl",
    _KtJvmInfo = "KtJvmInfo",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

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
            "%workspace_prefix%": ctx.workspace_name + "/",
        },
        is_executable = True,
    )

def _unify_jars(ctx):
    if bool(ctx.attr.jar):
        return struct(class_jar = ctx.file.jar, source_jar = ctx.file.srcjar, ijar = None)
    else:
        # Legacy handling.
        jars = []
        source_jars = [ctx.file.srcjar] if ctx.file.srcjar else []

        # TODO after a while remove the for block, the checks after it,and simplify the source-jar to jar allignment.
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
            fail("got more than one jar, this is an error create an issue: %s" % jars)
        if len(source_jars) > 1:
            fail("got more than one source jar. " +
                "Did you include both srcjar and a sources jar in the jars attribute?: " +
                jars)
            print(source_jars)
        return struct(class_jar = jars[0], source_jar = source_jars[0] if len(source_jars) == 1 else None, ijar = None)

def kt_jvm_import_impl(ctx):
    if bool(ctx.attr.jars) and bool(ctx.attr.jar):
        fail("Cannot use both jars= and jar= attribute.  Prefer jar=")

    artifact = _unify_jars(ctx)
    kt_info = _KtJvmInfo(
        module_name = _utils.derive_module_name(ctx),
        outputs = struct(
            jars = [artifact],
        ),
    )
    all_files = [artifact.class_jar] + ([artifact.source_jar] if bool(artifact.source_jar) else [])
    return struct(
        kt = kt_info,
        providers = [
            DefaultInfo(files = depset(all_files)),
            JavaInfo(
                output_jar = artifact.class_jar,
                compile_jar = artifact.class_jar,
                source_jar = artifact.source_jar,
                runtime_deps = [dep[JavaInfo] for dep in ctx.attr.runtime_deps if JavaInfo in dep],
                exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
                neverlink = getattr(ctx.attr, "neverlink", False),
            ),
            kt_info,
        ],
    )

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
            direct = ctx.files._java_runtime,
        ),
    )

_SPLIT_STRINGS = [
    "src/test/java/",
    "javatests/",
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
        jvm_flags = ["-ea", "-Dbazel.test_suite=%s" % test_class] + ctx.attr.jvm_flags,
    )
    return _make_providers(
        ctx,
        providers,
        depset(
            order = "default",
            transitive = [runtime_jars],
            direct = ctx.files._java_runtime,
        ),
    )
