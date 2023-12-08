KtCompilerPluginInfo = provider(
    fields = {
        "plugin_jars": "List of plugin jars.",
        "classpath": "The kotlin compiler plugin classpath.",
        "stubs": "Run this plugin during kapt stub generation.",
        "compile": "Run this plugin during koltinc compilation.",
        "options": "List of plugin options, represented as structs with an id and a value field, to be passed to the compiler",
    },
)

KtCompilerPluginOptions = provider(
    fields = {
        "plugin": "Provider of the plugin. Maybe KspPluginInfo or a JavaPluginInfo",
        "options": "List of plugin options, represented as structs with an id and a value field",
    },
)

KspPluginInfo = provider(
    fields = {
        "plugins": "List of JavaPluginInfo providers for the plugins to run with KSP",
    },
)
