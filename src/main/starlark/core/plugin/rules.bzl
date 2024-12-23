load("//src/main/starlark/core/compile:common.bzl", "TOOLCHAIN")
load(
    "//src/main/starlark/core/plugin:providers.bzl",
    "KtCompilerPluginClasspathInfo",
    "KtCompilerPluginInfo",
    "KtCompilerPluginOption",
    "KtPluginConfiguration",
)

def kt_compiler_deps_aspect_impl(target, ctx):
    """
    Collects and reshades (if necessary) all jars in the plugin transitive closure.

    Args:
        target: Target of the rule being inspected
        ctx: aspect ctx
    Returns:
        list of KtCompilerPluginClasspathInfo
    """
    transitive_infos = [
        t[KtCompilerPluginClasspathInfo]
        for d in ["deps", "runtime_deps", "exports"]
        for t in getattr(ctx.rule.attr, d, [])
        if KtCompilerPluginClasspathInfo in t
    ]
    reshaded_infos = []
    infos = [
        i
        for t in transitive_infos
        for i in t.infos
    ]
    if JavaInfo in target:
        ji = target[JavaInfo]
        infos.append(ji)

        reshaded_infos.append(
            _reshade_embedded_kotlinc_jars(
                target = target,
                ctx = ctx,
                jars = ji.runtime_output_jars,
                deps = [
                    i
                    for t in transitive_infos
                    for i in t.reshaded_infos
                ],
            ),
        )

    return [
        KtCompilerPluginClasspathInfo(
            reshaded_infos = reshaded_infos,
            infos = [java_common.merge(infos)],
        ),
    ]

def _reshade_embedded_kotlinc_jars(target, ctx, jars, deps):
    kt_plugin_tc = ctx.toolchains[TOOLCHAIN]

    reshaded = [
        kt_plugin_tc.reshade(
            actions = ctx.actions,
            input = jar,
            output = ctx.actions.declare_file(
                "%s_reshaded_%s" % (target.label.name, jar.basename),
            ),
        )
        for jar in jars
    ]

    # JavaInfo only takes a single jar, so create many and merge them.
    return java_common.merge(
        [
            JavaInfo(output_jar = jar, compile_jar = jar, deps = deps)
            for jar in reshaded
        ],
    )

def _resolve_plugin_options(id, string_list_dict, expand_location):
    """
    Resolves plugin options from a string dict to a dict of strings.

    Args:
        id: the plugin id
        string_list_dict: a dict of list[string].
    Returns:
        a dict of strings
    """
    options = []
    for (k, vs) in string_list_dict.items():
        for v in vs:
            if "=" in k:
                fail("kotlin compiler option keys cannot contain the = symbol")
            value = k + "=" + expand_location(v) if v else k
            options.append(KtCompilerPluginOption(id = id, value = value))
    return options

# This is naive reference implementation for resolving configurations.
# A more complicated plugin will need to provide its own implementation.
def _resolve_plugin_cfg(info, options, deps, expand_location):
    ji = java_common.merge([dep[JavaInfo] for dep in deps if JavaInfo in dep])
    classpath = depset(ji.runtime_output_jars, transitive = [ji.transitive_runtime_jars])
    return KtPluginConfiguration(
        id = info.id,
        options = _resolve_plugin_options(info.id, options, expand_location),
        classpath = classpath,
        data = depset(),
    )

def kt_compiler_plugin_impl(ctx):
    plugin_id = ctx.attr.id

    deps = ctx.attr.deps
    info = None
    if ctx.attr.target_embedded_compiler:
        info = java_common.merge([
            i
            for d in deps
            for i in d[KtCompilerPluginClasspathInfo].reshaded_infos
        ])
    else:
        info = java_common.merge([
            i
            for d in deps
            for i in d[KtCompilerPluginClasspathInfo].infos
        ])

    classpath = depset(info.runtime_output_jars, transitive = [info.transitive_runtime_jars])

    # TODO(1035): Migrate kt_compiler_plugin.options to string_list_dict
    options = _resolve_plugin_options(plugin_id, {k: [v] for (k, v) in ctx.attr.options.items()}, ctx.expand_location)

    return [
        DefaultInfo(files = classpath),
        KtCompilerPluginInfo(
            id = plugin_id,
            classpath = classpath,
            options = options,
            stubs = ctx.attr.stubs_phase,
            compile = ctx.attr.compile_phase,
            resolve_cfg = _resolve_plugin_cfg,
        ),
    ]

def kt_plugin_cfg_impl(ctx):
    plugin = ctx.attr.plugin[KtCompilerPluginInfo]
    return plugin.resolve_cfg(plugin, ctx.attr.options, ctx.attr.deps, ctx.expand_location)

kt_plugin_cfg = rule(
    implementation = kt_plugin_cfg_impl,
    doc = """
    Configurations for kt_compiler_plugin, ksp_plugin, and java_plugin.

    This allows setting options and dependencies independently from the initial plugin definition.
    """,
    attrs = {
        "plugin": attr.label(
            doc = "The plugin to associate with this configuration",
            providers = [KtCompilerPluginInfo],
            mandatory = True,
        ),
        "options": attr.string_list_dict(
            doc = "A dictionary of flag to values to be used as plugin configuration options.",
        ),
        "deps": attr.label_list(
            doc = "Dependencies for this configuration.",
            providers = [
                #[_KspPluginInfo],
                [JavaInfo],
                [JavaPluginInfo],
            ],
        ),
    },
)

kt_compiler_deps_aspect = aspect(
    implementation = kt_compiler_deps_aspect_impl,
    attr_aspects = ["deps", "runtime_deps", "exports"],
    toolchains = [
        TOOLCHAIN,
    ],
)

kt_compiler_plugin = rule(
    doc = """\
Define a plugin for the Kotlin compiler to run. The plugin can then be referenced in the `plugins` attribute
of the `kt_jvm_*` rules.

An example can be found under `//examples/plugin`:

```bzl
kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": "plugin.OpenForTesting",
    },
    deps = [
        "//kotlin/compiler:allopen-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "open_for_testing",
    srcs = ["OpenForTesting.kt"],
)

kt_jvm_library(
    name = "user",
    srcs = ["User.kt"],
    plugins = [":open_for_testing_plugin"],
    deps = [
        ":open_for_testing",
    ],
)
```
""",
    attrs = {
        "deps": attr.label_list(
            doc = "The list of libraries to be added to the compiler's plugin classpath",
            providers = [JavaInfo],
            cfg = "exec",
            aspects = [kt_compiler_deps_aspect],
        ),
        "id": attr.string(
            doc = "The ID of the plugin",
            mandatory = True,
        ),
        "options": attr.string_dict(
            doc = """\
Dictionary of options to be passed to the plugin.
Supports the following template values:

- `{generatedClasses}`: directory for generated class output
- `{temp}`: temporary directory, discarded between invocations
- `{generatedSources}`:  directory for generated source output
- `{classpath}` : replaced with a list of jars separated by the filesystem appropriate separator.
""",
            default = {},
        ),
        "compile_phase": attr.bool(
            doc = "Runs the compiler plugin during kotlin compilation. Known examples: `allopen`, `sam_with_reciever`",
            default = True,
        ),
        "stubs_phase": attr.bool(
            doc = "Runs the compiler plugin in kapt stub generation.",
            default = True,
        ),
        "target_embedded_compiler": attr.bool(
            doc = """Plugin was compiled against the embeddable kotlin compiler. These plugins expect shaded kotlinc
            dependencies, and will fail when running against a non-embeddable compiler.""",
            default = False,
        ),
    },
    implementation = kt_compiler_plugin_impl,
    provides = [KtCompilerPluginInfo],
    toolchains = [
        TOOLCHAIN,
    ],
)
