TYPE = "//src/main/starlark/core/compile:toolchain_type"

# Java toolchains
JAVA_TOOLCHAIN_TYPE = Label("@bazel_tools//tools/jdk:toolchain_type")
JAVA_RUNTIME_TOOLCHAIN_TYPE = Label("@bazel_tools//tools/jdk:runtime_toolchain_type")

KtJvmInfo = provider(
    fields = {
        "additional_generated_source_jars": "Returns additional Jars containing generated source files from kapt, ksp, etc. [bazel-bsp-aspect]",
        "all_output_jars": "Returns all the output Jars produced by this rule. [bazel-bsp-aspect]",
        "annotation_processing": "Generated annotation processing jars. [intellij-aspect]",
        "classpath_snapshot": "Classpath snapshot file for incremental compilation (None if IC disabled)",
        "exported_compiler_plugins": "compiler plugins to be invoked by targets depending on this.",
        "language_version": "version of kotlin used. [intellij-aspect]",
        "module_jars": "Jars comprising the module (logical compilation unit), a.k.a. associates",
        "module_name": "the module name",
        "outputs": "output jars produced by this rule. [intelij-aspect]",
        "srcs": "the source files. [intelij-aspect]",
        "transitive_compile_time_jars": "Returns the transitive set of Jars required to build the target. [intellij-aspect]",
        "transitive_source_jars": "Returns the Jars containing source files of the current target and all of its transitive dependencies. [intellij-aspect]",
    },
)
