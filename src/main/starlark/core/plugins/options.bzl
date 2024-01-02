load(":providers.bzl", "KspPluginInfo", "KtCompilerPluginInfo", "KtPluginConfiguration")

_DEPS_PROVIDERS = [
    JavaInfo,
    JavaPluginInfo,
    KspPluginInfo,
]

def _kt_plugin_cfg(ctx):
    options = []
    (id, plugin) = _find_plugin_provider_and_id(ctx.attrs.plugin)
    return KtPluginConfiguration(
        plugin = plugin,
        options = [struct(
            id = id,
            value = "=".join(kv),
        ) for kv in ctx.attr.options.items()],
        deps = [
            dep[p]
            for p in _DEPS_PROVIDERS
            for dep in ctx.deps
            if p in dep
        ],
    )

def _find_plugin_provider_and_id(plugin):
    if KspPluginInfo in plugin:
        return ("com.google.devtools.ksp.symbol-processing", plugin[KspPluginInfo])
    if JavaPluginInfo in plugin:
        return ("org.jetbrains.kotlin.kapt3", plugin[JavaPluginInfo])
    if KtCompilerPluginInfo in plugin:
        return (plugin[KtCompilerPluginInfo].id, plugin[KtCompilerPluginInfo])

# Represents a specific configuration for a plugin
kt_plugin_cfg = rule(
    implementation = _kt_plugin_cfg,
    attrs = {
        "plugin": attr.label(
            doc = "The plugin to associate with this configuration",
            providers = [
                [KtCompilerPluginInfo],
                [KspPluginInfo],
                [JavaPluginInfo],
            ],
        ),
        "options": attr.string_dict(
            doc = "A list of plugin configuration options.",
        ),
        "deps": attr.label_list(
            doc = "Dependencies for this configuration.",
            providers = [[d] for d in _DEPS_PROVIDERS],
        ),
    },
)
