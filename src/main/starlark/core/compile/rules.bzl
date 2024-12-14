load("@rules_java//java:defs.bzl", "JavaInfo")
load(":common.bzl", "KtJvmInfo", "TYPE")

_COMMON_ATTRS = {
    "srcs": attr.label_list(
        doc = """The list of source files that are processed to create the target, this can contain both Java and Kotlin
                       files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.""",
        default = [],
        allow_files = [".kt", ".java"],
    ),
    "deps": attr.label_list(
        doc = """A list of dependencies of this rule. See general comments about `deps` at
                       [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).""",
        providers = [
            [JavaInfo],
            [KtJvmInfo],
        ],
        allow_files = False,
    ),
    "exports": attr.label_list(
        doc = """\
    Exported libraries.

    Deps listed here will be made available to other rules, as if the parents explicitly depended on
    these deps. This is not true for regular (non-exported) deps.""",
        default = [],
        providers = [JavaInfo, KtJvmInfo],
    ),
    "neverlink": attr.bool(
        doc = """If true only use this library for compilation and not at runtime.""",
        default = False,
    ),
    "runtime_deps": attr.label_list(
        doc = """Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will
                       appear on the runtime classpath, but unlike them, not on the compile-time classpath.""",
        default = [],
        allow_files = False,
    ),
    "resources": attr.label_list(
        doc = """A list of files that should be include in a Java jar.""",
        default = [],
        allow_files = True,
    ),
    "data": attr.label_list(
        doc = """A list of files that should be include the runfiles.""",
        default = [],
        allow_files = True,
    ),
    "kotlinc_opts": attr.string_dict(doc = "Options to pass to the kotlinc compiler as key: value pairs."),
    "module_name": attr.string(
        doc = """The name of the module, if not provided the module name is derived from the label. --e.g.,
                               `//some/package/path:label_name` is translated to
                               `some_package_path-label_name`.""",
        mandatory = False,
    ),
    "class_jar": attr.output(doc = "jar containing .kt and .java class files"),
    "source_jar": attr.output(doc = "jar containing .kt and .java sources"),
}

def _kt_jvm_library_impl(ctx):
    kt_tools = ctx.toolchains[TYPE]
    class_jar = ctx.outputs.class_jar
    source_jar = ctx.outputs.source_jar
    java_info_deps = [d[JavaInfo] for d in ctx.attr.deps if JavaInfo in d]
    module_name = ctx.attr.module_name or str(ctx.label).lstrip("/").replace("/", "_").replace(":", "-").replace("@", "")
    kt_tools.compile(
        actions = ctx.actions,
        srcs = ctx.files.srcs,
        dep_jars = depset(transitive = [j.compile_jars for j in java_info_deps]),
        class_jar = class_jar,
        output_srcjar = source_jar,
        module_name = module_name,
        path_separator = ctx.configuration.host_path_separator,
        kotlinc_opts = ctx.attr.kotlinc_opts,
    )

    java_info = JavaInfo(
        compile_jar = class_jar,
        output_jar = class_jar,
        source_jar = source_jar,
        deps = java_info_deps,
        neverlink = ctx.attr.neverlink,
        exports = [e[JavaInfo] for e in ctx.attr.exports],
    )
    return [
        KtJvmInfo(
            module_name = module_name,
            module_jars = depset([class_jar]),
            srcs = ctx.files.srcs,
            outputs = struct(
                jdeps = None,
                jars = [struct(
                    class_jar = class_jar,
                    ijar = class_jar,
                    source_jars = [source_jar],
                )],
            ),
            transitive_compile_time_jars = java_info.transitive_compile_time_jars,
            transitive_source_jars = java_info.transitive_source_jars,
            annotation_processing = None,
            additional_generated_source_jars = [],
            all_output_jars = [class_jar, source_jar],
        ),
        java_info,
        DefaultInfo(
            files = depset([class_jar]),
            runfiles = ctx.runfiles(
                # explicitly include data files, otherwise they appear to be missing
                files = ctx.files.data,
            ).merge_all([
                d[DefaultInfo].default_runfiles
                for d in ctx.attr.deps
                if DefaultInfo in d
            ]),
        ),
    ]

_kt_jvm_library = rule(
    implementation = _kt_jvm_library_impl,
    attrs = _COMMON_ATTRS,
    toolchains = [
        TYPE,
    ],
    provides = [JavaInfo, KtJvmInfo],
)

def core_kt_jvm_library(name, **kwargs):
    _kt_jvm_library(
        name = name,
        class_jar = "%slib.jar" % name,
        source_jar = "%slib.srcjar" % name,
        **kwargs
    )

def _kt_jvm_binary_impl(ctx):
    kt_tools = ctx.toolchains[TYPE]

    providers = _kt_jvm_library_impl(ctx)
    java_info_deps = [d[JavaInfo] for d in ctx.attr.deps if JavaInfo in d]
    runtime_jars = depset([ctx.outputs.class_jar], transitive = [j.transitive_runtime_jars for j in java_info_deps])
    executable = ctx.outputs.executable

    launch_runfiles = kt_tools.launch(
        main_class = ctx.attr.main_class,
        executable_output = executable,
        actions = ctx.actions,
        path_separator = ctx.configuration.host_path_separator,
        workspace_prefix = ctx.workspace_name + "/",
        jvm_flags = " ".join([ctx.expand_location(f, ctx.attr.data) for f in ctx.attr.jvm_flags]),
        runtime_jars = runtime_jars,
    )

    kt_tools.deploy(
        actions = ctx.actions,
        jars = runtime_jars,
        output_jar = ctx.outputs.deploy_jar,
    )

    return [p for p in providers if type(p) != type(DefaultInfo())] + [
        DefaultInfo(
            files = depset([executable]),
            runfiles = ctx.runfiles(
                # explicitly include data files, otherwise they appear to be missing
                files = ctx.files.data,
                transitive_files = launch_runfiles,
            ).merge_all([
                d[DefaultInfo].default_runfiles
                for d in ctx.attr.deps
                if DefaultInfo in d
            ]),
            executable = executable,
        ),
    ]

_kt_jvm_binary = rule(
    implementation = _kt_jvm_binary_impl,
    attrs = {
        k: v
        for (k, v) in _COMMON_ATTRS.items() + {
            "jvm_flags": attr.string_list(default = []),
            "main_class": attr.string(mandatory = True, doc = ""),
            "deploy_jar": attr.output(doc = "jar containing all dependencies."),
        }.items()
    },
    toolchains = [
        TYPE,
    ],
    executable = True,
)

def core_kt_jvm_binary(name, **kwargs):
    _kt_jvm_binary(
        name = name,
        class_jar = "%slib.jar" % name,
        source_jar = "%slib.srcjar" % name,
        deploy_jar = "%s_deploy.jar" % name,
        **kwargs
    )
