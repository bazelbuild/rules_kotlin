KtCompilerPluginOption = provider(
    fields = {
        "id": "The id of the plugin this option belongs to.",
        "key": "The option key.",
        "value": "The option value.",
    },
)

KtCompilerPluginInfo = provider(
    fields = {
        "classpath": "The kotlin compiler plugin classpath.",
        "compile": "Run this plugin during koltinc compilation.",
        "id": "The id of the plugin.",
        "merge_cfgs": "A Callable[[KtCompilerPluginInfo, List[KtPluginConfiguration]]] that merge multiple plugin configurations.",
        "options": "List of plugin options, represented as KtCompilerPluginOption, to be passed to the compiler",
        "plugin_jars": "List of plugin jars.",
        "resolve_cfg": "A Callable[[KtCompilerPluginInfo, Dict[str,str], List[Target], KtPluginConfiguration]" +
                       " that resolves an associated plugin configuration.",
        "stubs": "Run this plugin during kapt stub generation.",
    },
)

KtPluginConfiguration = provider(
    fields = {
        "classpath": "Depset of jars to add to the classpath when running the plugin.",
        "data": "runfiles to pass to the plugin.",
        "id": "The id of the compiler plugin associated with this configuration.",
        "options": "List of plugin options, represented KtCompilerPluginOption",
    },
)

KspPluginInfo = provider(
    fields = {
        "generates_java": "Runs Java compilation action for this plugin",
        "plugins": "List of JavaPluginInfo providers for the plugins to run with KSP",
    },
)
