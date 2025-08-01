KtCompilerPluginOption = provider(
    fields = {
        "id": "The id of the option.",
        "value": "The value of the option.",
    },
)

KtCompilerPluginInfo = provider(
    fields = {
        "id": "The id of the plugin.",
        "plugin_jars": "List of plugin jars.",
        "classpath": "The kotlin compiler plugin classpath.",
        "stubs": "Run this plugin during kapt stub generation.",
        "compile": "Run this plugin during koltinc compilation.",
        "options": "List of plugin options, represented as KtCompilerPluginOption, to be passed to the compiler",
        "resolve_cfg": "A Callable[[KtCompilerPluginInfo, Dict[str,str], List[Target], KtPluginConfiguration]" +
                       " that resolves an associated plugin configuration.",
        "merge_cfgs": "A Callable[[KtCompilerPluginInfo, List[KtPluginConfiguration]]] that merge multiple plugin configurations.",
    },
)

KtPluginConfiguration = provider(
    fields = {
        "id": "The id of the compiler plugin associated with this configuration.",
        "options": "List of plugin options, represented KtCompilerPluginOption",
        "classpath": "Depset of jars to add to the classpath when running the plugin.",
        "data": "runfiles to pass to the plugin.",
    },
)

KspPluginInfo = provider(
    fields = {
        "plugins": "List of JavaPluginInfo providers for the plugins to run with KSP",
        "generates_java": "Runs Java compilation action for this plugin",
    },
)
