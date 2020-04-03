load(
    "//kotlin/internal:defs.bzl",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
)

def plugins_to_classpaths(providers_list):
    flattened_files = []
    for providers in providers_list:
        if _KtCompilerPluginInfo in providers:
            provider = providers[_KtCompilerPluginInfo]
            for e in provider.classpath:
                flattened_files.append(e)
    return flattened_files

def plugins_to_options(providers_list):
    kt_compiler_plugin_providers = [providers[_KtCompilerPluginInfo] for providers in providers_list if _KtCompilerPluginInfo in providers]
    flattened_options = []
    for provider in kt_compiler_plugin_providers:
        for option in provider.options:
            flattened_options.append("%s:%s" % (option.id, option.value))
    return flattened_options
