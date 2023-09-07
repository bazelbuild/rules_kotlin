# The core kotlin providers are returned from the kotlinc repository in use.
# DO NOT IMPORT on this file.
KtJvmInfo = provider(
    fields = {
        "module_name": "the module name",
        "module_jars": "Jars comprising the module (logical compilation unit), a.k.a. associates",
        "exported_compiler_plugins": "compiler plugins to be invoked by targets depending on this.",
        "srcs": "the source files. [intelij-aspect]",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
        "language_version": "version of kotlin used. [intellij-aspect]",
        "transitive_compile_time_jars": "Returns the transitive set of Jars required to build the target. [intellij-aspect]",
        "transitive_source_jars": "Returns the Jars containing source files of the current target and all of its transitive dependencies. [intellij-aspect]",
        "annotation_processing": "Generated annotation processing jars. [intellij-aspect]",
    },
)

KtJsInfo = provider(
    fields = {
        "js": "The primary output of the library",
        "js_map": "The map file for the library",
        "jar": "A jar of the library.",
        "srcjar": "The jar containing the sources of the library",
    },
)

KtCompilerPluginInfo = provider(
    fields = {
        "plugin_jars": "List of plugin jars.",
        "classpath": "The kotlin compiler plugin classpath.",
        "stubs": "Run this plugin during kapt stub generation.",
        "compile": "Run this plugin during koltinc compilation.",
        "options": "List of plugin options, represented as structs with an id and a value field, to be passed to the compiler",
    },
)

KspPluginInfo = provider(
    fields = {
        "plugins": "List of JavaPLuginInfo providers for the plugins to run with KSP",
    },
)
