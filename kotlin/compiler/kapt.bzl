load(
    "//kotlin/internal:defs.bzl",
    "KtCompilerPluginInfo",
    "KtCompilerPluginOption",
    "KtPluginConfiguration",
)

# This is naive reference implementation for resolving configurations.
# A more complicated plugin will need to provide its own implementation.
def _resolve_plugin_cfg(info, options, deps, expand_location):
    ji = java_common.merge([dep[JavaInfo] for dep in deps if JavaInfo in dep])
    classpath = depset(ji.runtime_output_jars, transitive = [ji.transitive_runtime_jars])

    apoptions = options.get("apoptions", [])

    return KtPluginConfiguration(
        id = info.id,
        options = _resolve_plugin_options(info.id, options, expand_location),
        classpath = classpath,
        data = depset(),
    )

def _kapt_compiler_plugin_impl(ctx):
    plugin_id = ctx.attr.id

    deps = ctx.attr.deps
    info = None

    options = _resolve_plugin_options(plugin_id, ctx.attr.options, ctx.expand_location)

    return [
        DefaultInfo(files = classpath),
        KtCompilerPluginInfo(
            id = plugin_id,
            classpath = depset(),
            options = options,
            stubs = True,
            compile = False,
            resolve_cfg = _resolve_plugin_cfg,
        ),
    ]

kapt_compiler_plugin = rule(
    implementation = _kapt_compiler_plugin_impl,
    attrs = {
        "id": attr.string(default = ""),
        "options": attr.string_list_dict(),
    },
)
